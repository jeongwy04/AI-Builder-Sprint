-- ============================================================================
-- Trace Archive — 최초 마이그레이션
-- 8테이블 + is_member() + RLS(테넌트 4종) + Storage 정책 + 검색/초대 RPC
--
-- 규칙 근거: CLAUDE.md §5(아키텍처 불변 규칙) · §7(데이터 모델) · §8(RPC 규약)
-- 실행 순서 주의:
--   확장 → 테이블 정의 → is_member() → RLS 활성화/정책 → Storage → RPC
--   (language sql 함수는 생성 시 본문이 검증되므로 참조 테이블이 먼저 있어야 한다)
--
-- ⚠️ TODO(선행 확인): embedding 차원.
--   Upstage embedding-passage 출력 차원 = 4096 (문서/블로그 기준).
--   콘솔 curl로 최종 확인 후 값이 다르면 VECTOR(4096) 한 곳만 수정.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. 확장
-- ----------------------------------------------------------------------------
create extension if not exists vector;

-- ----------------------------------------------------------------------------
-- 1. profiles  (auth.users 1:1 — users 테이블 직접 생성 금지)
-- ----------------------------------------------------------------------------
create table public.profiles (
    id           uuid primary key references auth.users(id) on delete cascade,
    display_name text,
    avatar_url   text,
    created_at   timestamptz not null default now()
);

-- 회원가입 시 프로필 자동 생성 (Supabase 표준 패턴)
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, display_name)
    values (
        new.id,
        coalesce(new.raw_user_meta_data ->> 'name', split_part(new.email, '@', 1))
    );
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ----------------------------------------------------------------------------
-- 2. archives  (그룹 = 보관소. 자신이 테넌트 루트)
-- ----------------------------------------------------------------------------
create table public.archives (
    id               uuid primary key default gen_random_uuid(),
    name             text not null,
    group_type       text,                       -- family / couple / friends / club
    cover_image_path text,
    created_by       uuid not null references auth.users(id) default auth.uid(),
    created_at       timestamptz not null default now()
);

-- ----------------------------------------------------------------------------
-- 3. memberships  (⚠️ role 컬럼 없음 — 전원 동일 권한)
-- ----------------------------------------------------------------------------
create table public.memberships (
    id         uuid primary key default gen_random_uuid(),
    archive_id uuid not null references public.archives(id) on delete cascade,
    user_id    uuid not null references auth.users(id) on delete cascade,
    joined_at  timestamptz not null default now(),
    unique (archive_id, user_id)
);

-- ----------------------------------------------------------------------------
-- 4. invitations  (초대 토큰)
-- ----------------------------------------------------------------------------
create table public.invitations (
    id         uuid primary key default gen_random_uuid(),
    archive_id uuid not null references public.archives(id) on delete cascade,
    token      text not null unique default encode(gen_random_bytes(16), 'hex'),
    expires_at timestamptz not null default (now() + interval '7 days'),
    used_count int not null default 0,
    created_by uuid not null references auth.users(id) default auth.uid(),
    created_at timestamptz not null default now()
);

-- ----------------------------------------------------------------------------
-- 5. memories  (기억 단위 + 검색 인덱스)
-- ----------------------------------------------------------------------------
create table public.memories (
    id          uuid primary key default gen_random_uuid(),
    archive_id  uuid not null references public.archives(id) on delete cascade,
    author_id   uuid not null references auth.users(id) default auth.uid(),
    memory_date date not null,                    -- 추억이 일어난 날 (정렬·검색 기준)
    place_name  text,
    lat         double precision,
    lng         double precision,
    search_text text,                             -- 메모 전부 + 장소 + 날짜표현 (embed-memory가 조립)
    embedding   vector(4096),                     -- TODO: Embed 차원 확인 (§상단 주석)
    created_at  timestamptz not null default now()  -- 업로드 시각
);

create index memories_archive_date_idx
    on public.memories (archive_id, memory_date desc);
-- pgvector 인덱스는 현 단계에서 만들지 않는다 (규모상 순차 스캔으로 충분 — CLAUDE.md §7)

-- ----------------------------------------------------------------------------
-- 6. media_assets  (Storage 미디어 메타)
-- ----------------------------------------------------------------------------
create table public.media_assets (
    id           uuid primary key default gen_random_uuid(),
    memory_id    uuid not null references public.memories(id) on delete cascade,
    archive_id   uuid not null references public.archives(id) on delete cascade,
    storage_path text not null,                   -- {archive_id}/{memory_id}/{uuid}.{ext}
    media_type   text not null,                   -- image / video
    mime_type    text,
    size_bytes   bigint,
    duration_sec double precision,
    exif         jsonb,
    created_at   timestamptz not null default now()
);

create index media_assets_memory_idx on public.media_assets (memory_id);

-- ----------------------------------------------------------------------------
-- 7. notes  (메모·문구 — memory 1:N. 검색의 유일한 재료)
-- ----------------------------------------------------------------------------
create table public.notes (
    id         uuid primary key default gen_random_uuid(),
    memory_id  uuid not null references public.memories(id) on delete cascade,
    archive_id uuid not null references public.archives(id) on delete cascade,
    author_id  uuid not null references auth.users(id) default auth.uid(),
    body       text not null,
    created_at timestamptz not null default now()
);

create index notes_memory_idx on public.notes (memory_id);

-- ----------------------------------------------------------------------------
-- 8. chat_sessions / chat_messages  (AI 대화 이력 — 사용자 개인 스코프)
-- ----------------------------------------------------------------------------
create table public.chat_sessions (
    id         uuid primary key default gen_random_uuid(),
    archive_id uuid not null references public.archives(id) on delete cascade,
    user_id    uuid not null references auth.users(id) on delete cascade default auth.uid(),
    created_at timestamptz not null default now()
);

create table public.chat_messages (
    id                uuid primary key default gen_random_uuid(),
    session_id        uuid not null references public.chat_sessions(id) on delete cascade,
    archive_id        uuid not null references public.archives(id) on delete cascade,
    role              text not null check (role in ('user', 'assistant', 'tool')),
    content           text,
    tool_calls        jsonb,
    result_memory_ids uuid[],
    created_at        timestamptz not null default now()
);

create index chat_messages_session_idx on public.chat_messages (session_id, created_at);

-- ============================================================================
-- 9. is_member()  — RLS 멤버십 판정 헬퍼
--    security definer 필수: 정책 안에서 memberships를 직접 조회하면 무한 재귀.
-- ============================================================================
create or replace function public.is_member(p_archive_id uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
    select exists (
        select 1 from public.memberships
        where archive_id = p_archive_id
          and user_id = auth.uid()
    );
$$;

-- ============================================================================
-- 10. RLS 활성화 + 정책
-- ============================================================================

-- 10.1 profiles ---------------------------------------------------------------
alter table public.profiles enable row level security;

-- 앱 내에서 작성자 이름 표시를 위해 로그인 사용자는 프로필 조회 허용
create policy "profiles_select_authenticated" on public.profiles
    for select to authenticated using (true);
create policy "profiles_update_self" on public.profiles
    for update using (id = auth.uid());

-- 10.2 archives ---------------------------------------------------------------
alter table public.archives enable row level security;

create policy "archives_select_member" on public.archives
    for select using (public.is_member(id));
-- 누구나 새 보관소 생성 가능 (생성 직후 membership을 같은 트랜잭션에서 추가)
create policy "archives_insert_authenticated" on public.archives
    for insert to authenticated with check (created_by = auth.uid());
create policy "archives_update_member" on public.archives
    for update using (public.is_member(id));
create policy "archives_delete_member" on public.archives
    for delete using (public.is_member(id));

-- 10.3 memberships ------------------------------------------------------------
alter table public.memberships enable row level security;

-- 멤버는 같은 보관소의 멤버 목록을 볼 수 있다
create policy "memberships_select_member" on public.memberships
    for select using (public.is_member(archive_id));
-- 본인 멤버십만 직접 추가 가능 (보관소 생성 시 자기 등록 / 초대 수락은 RPC 사용)
create policy "memberships_insert_self" on public.memberships
    for insert to authenticated with check (user_id = auth.uid());
-- 본인 멤버십만 탈퇴(삭제) 가능
create policy "memberships_delete_self" on public.memberships
    for delete using (user_id = auth.uid());

-- 10.4 invitations ------------------------------------------------------------
alter table public.invitations enable row level security;

create policy "invitations_select_member" on public.invitations
    for select using (public.is_member(archive_id));
create policy "invitations_insert_member" on public.invitations
    for insert to authenticated with check (public.is_member(archive_id));
create policy "invitations_delete_member" on public.invitations
    for delete using (public.is_member(archive_id));

-- 10.5 테넌트 4종 정책 (memories / media_assets / notes) ----------------------
-- 동일 패턴 반복. archive_id 기준 is_member() 판정.

alter table public.memories enable row level security;
create policy "memories_select" on public.memories
    for select using (public.is_member(archive_id));
create policy "memories_insert" on public.memories
    for insert with check (public.is_member(archive_id));
create policy "memories_update" on public.memories
    for update using (public.is_member(archive_id));
create policy "memories_delete" on public.memories
    for delete using (public.is_member(archive_id));

alter table public.media_assets enable row level security;
create policy "media_assets_select" on public.media_assets
    for select using (public.is_member(archive_id));
create policy "media_assets_insert" on public.media_assets
    for insert with check (public.is_member(archive_id));
create policy "media_assets_update" on public.media_assets
    for update using (public.is_member(archive_id));
create policy "media_assets_delete" on public.media_assets
    for delete using (public.is_member(archive_id));

alter table public.notes enable row level security;
create policy "notes_select" on public.notes
    for select using (public.is_member(archive_id));
create policy "notes_insert" on public.notes
    for insert with check (public.is_member(archive_id));
create policy "notes_update" on public.notes
    for update using (public.is_member(archive_id));
create policy "notes_delete" on public.notes
    for delete using (public.is_member(archive_id));

-- 10.6 chat_sessions / chat_messages (사용자 개인 스코프) ----------------------
-- 대화는 그룹 공유가 아니라 사용자 개인 것이다. 멤버십 + 본인 소유 둘 다 요구.
alter table public.chat_sessions enable row level security;
create policy "chat_sessions_select_own" on public.chat_sessions
    for select using (user_id = auth.uid());
create policy "chat_sessions_insert_own" on public.chat_sessions
    for insert to authenticated
    with check (user_id = auth.uid() and public.is_member(archive_id));
create policy "chat_sessions_delete_own" on public.chat_sessions
    for delete using (user_id = auth.uid());

alter table public.chat_messages enable row level security;
-- 부모 세션 소유자만 접근 (다른 테이블 참조라 재귀 없음)
create policy "chat_messages_select_own" on public.chat_messages
    for select using (
        exists (
            select 1 from public.chat_sessions s
            where s.id = session_id and s.user_id = auth.uid()
        )
    );
create policy "chat_messages_insert_own" on public.chat_messages
    for insert to authenticated with check (
        exists (
            select 1 from public.chat_sessions s
            where s.id = session_id and s.user_id = auth.uid()
        )
    );

-- ============================================================================
-- 11. Storage — private 버킷 + 멤버 격리 정책
--    경로 규칙: {archive_id}/{memory_id}/{uuid}.{ext}
--    → foldername[1] 이 archive_id
-- ============================================================================
insert into storage.buckets (id, name, public)
values ('memories', 'memories', false)
on conflict (id) do nothing;

create policy "media_select_member" on storage.objects
    for select using (
        bucket_id = 'memories'
        and public.is_member(((storage.foldername(name))[1])::uuid)
    );
create policy "media_insert_member" on storage.objects
    for insert to authenticated with check (
        bucket_id = 'memories'
        and public.is_member(((storage.foldername(name))[1])::uuid)
    );
create policy "media_delete_member" on storage.objects
    for delete using (
        bucket_id = 'memories'
        and public.is_member(((storage.foldername(name))[1])::uuid)
    );

-- ============================================================================
-- 12. match_memories()  — 메타 필터 + 벡터 유사도 하이브리드 검색
--    security definer 붙이지 않는다 → 호출자 권한으로 실행되어 RLS 적용. (CLAUDE.md §8.3)
-- ============================================================================
create or replace function public.match_memories(
    p_archive_id     uuid,
    p_query_embedding vector,
    p_date_from      date default null,
    p_date_to        date default null,
    p_limit          int  default 10
)
returns setof public.memories
language sql
stable
as $$
    select * from public.memories
    where archive_id = p_archive_id
      and (p_date_from is null or memory_date >= p_date_from)
      and (p_date_to   is null or memory_date <= p_date_to)
      and embedding is not null
    order by embedding <=> p_query_embedding
    limit p_limit;
$$;

-- ============================================================================
-- 13. accept_invitation()  — 초대 토큰으로 멤버십 등록
--    비멤버가 토큰만으로 가입해야 하므로 security definer로 검증 후 삽입.
--    (invitations는 멤버만 select 가능 → 가입 흐름은 이 RPC를 통한다)
-- ============================================================================
create or replace function public.accept_invitation(p_token text)
returns uuid                     -- 가입된 archive_id 반환
language plpgsql
security definer
set search_path = public
as $$
declare
    v_archive_id uuid;
begin
    select archive_id into v_archive_id
    from public.invitations
    where token = p_token
      and expires_at > now()
    limit 1;

    if v_archive_id is null then
        raise exception 'INVITATION_INVALID' using errcode = 'P0001';
    end if;

    insert into public.memberships (archive_id, user_id)
    values (v_archive_id, auth.uid())
    on conflict (archive_id, user_id) do nothing;

    update public.invitations
    set used_count = used_count + 1
    where token = p_token;

    return v_archive_id;
end;
$$;

-- 로그인 사용자만 호출 가능
revoke all on function public.accept_invitation(text) from public, anon;
grant execute on function public.accept_invitation(text) to authenticated;
