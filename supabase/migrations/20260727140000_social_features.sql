-- ============================================================================
-- 소셜 기능 추가 — 멤버 그룹 채팅 · 안 읽음 배지 · 좋아요 · 함께한 사람 태깅
--
-- UI 확정에 따라 추가된 범위. (즐겨찾기/활발한랭킹/고정글은 제외)
-- 실시간(SSE) 푸시는 여전히 미구현 — 채팅은 일반 insert + 폴링/새로고침으로 처리.
-- 모든 테넌트 테이블은 archive_id + RLS 4종(또는 소유자 기반) 패턴을 따른다.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. messages — 멤버끼리 주고받는 그룹 채팅 메시지
--    (AI 대화용 chat_messages 와는 별개다)
-- ----------------------------------------------------------------------------
create table public.messages (
    id         uuid primary key default gen_random_uuid(),
    archive_id uuid not null references public.archives(id) on delete cascade,
    sender_id  uuid not null references auth.users(id) default auth.uid(),
    body       text,
    image_path text,                       -- Storage: {archive_id}/chat/{uuid}.{ext}
    created_at timestamptz not null default now(),
    check (body is not null or image_path is not null)
);
create index messages_archive_idx on public.messages (archive_id, created_at desc);

alter table public.messages enable row level security;
create policy "messages_select" on public.messages
    for select using (public.is_member(archive_id));
create policy "messages_insert" on public.messages
    for insert to authenticated
    with check (public.is_member(archive_id) and sender_id = auth.uid());
create policy "messages_delete_own" on public.messages
    for delete using (sender_id = auth.uid());

-- ----------------------------------------------------------------------------
-- 2. chat_reads — 사용자가 각 채팅방을 마지막으로 읽은 시점
--    안 읽음 개수 = 이 시점 이후 들어온, 내가 보내지 않은 메시지 수
-- ----------------------------------------------------------------------------
create table public.chat_reads (
    archive_id   uuid not null references public.archives(id) on delete cascade,
    user_id      uuid not null references auth.users(id) default auth.uid(),
    last_read_at timestamptz not null default now(),
    primary key (archive_id, user_id)
);

alter table public.chat_reads enable row level security;
-- 본인 읽음 기록만 다룬다. 앱은 방을 열 때 upsert(last_read_at = now()).
create policy "chat_reads_select_own" on public.chat_reads
    for select using (user_id = auth.uid());
create policy "chat_reads_upsert_own" on public.chat_reads
    for insert to authenticated
    with check (user_id = auth.uid() and public.is_member(archive_id));
create policy "chat_reads_update_own" on public.chat_reads
    for update using (user_id = auth.uid());

-- 홈 화면용: 내 모든 채팅방의 안 읽음 개수를 한 번에.
-- security definer 미부착 → 호출자 RLS로 messages가 멤버 방으로 이미 제한됨.
create or replace function public.unread_counts()
returns table(archive_id uuid, unread int)
language sql
stable
as $$
    select m.archive_id, count(*)::int as unread
    from public.messages m
    left join public.chat_reads r
      on r.archive_id = m.archive_id and r.user_id = auth.uid()
    where m.sender_id <> auth.uid()
      and (r.last_read_at is null or m.created_at > r.last_read_at)
    group by m.archive_id;
$$;
revoke all on function public.unread_counts() from public, anon;
grant execute on function public.unread_counts() to authenticated;

-- ----------------------------------------------------------------------------
-- 3. reactions — 게시물(memory)에 대한 좋아요/하트
-- ----------------------------------------------------------------------------
create table public.reactions (
    id         uuid primary key default gen_random_uuid(),
    memory_id  uuid not null references public.memories(id) on delete cascade,
    archive_id uuid not null references public.archives(id) on delete cascade,
    user_id    uuid not null references auth.users(id) default auth.uid(),
    created_at timestamptz not null default now(),
    unique (memory_id, user_id)            -- 한 사람당 한 번
);
create index reactions_memory_idx on public.reactions (memory_id);

alter table public.reactions enable row level security;
create policy "reactions_select" on public.reactions
    for select using (public.is_member(archive_id));
create policy "reactions_insert_own" on public.reactions
    for insert to authenticated
    with check (public.is_member(archive_id) and user_id = auth.uid());
create policy "reactions_delete_own" on public.reactions
    for delete using (user_id = auth.uid());

-- ----------------------------------------------------------------------------
-- 4. memory_people — 한 기억에 "함께한 사람"(멤버) 태깅
-- ----------------------------------------------------------------------------
create table public.memory_people (
    memory_id  uuid not null references public.memories(id) on delete cascade,
    archive_id uuid not null references public.archives(id) on delete cascade,
    user_id    uuid not null references auth.users(id) on delete cascade,
    primary key (memory_id, user_id)
);

alter table public.memory_people enable row level security;
create policy "memory_people_select" on public.memory_people
    for select using (public.is_member(archive_id));
create policy "memory_people_insert" on public.memory_people
    for insert to authenticated with check (public.is_member(archive_id));
create policy "memory_people_delete" on public.memory_people
    for delete using (public.is_member(archive_id));
