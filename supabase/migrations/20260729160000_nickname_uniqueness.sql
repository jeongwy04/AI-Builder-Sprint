-- ============================================================================
-- 회원가입 닉네임 중복 방지
--
-- 이메일 중복은 auth.users.email 자체가 이미 유니크라 Supabase Auth 가 signUp 단계에서
-- 알아서 막아준다(에러 코드 user_already_exists) — 앱(SupabaseAuthRepository)에서 그 에러를
-- 잡아 친절한 메시지로 바꾸기만 하면 되고, 스키마 변경은 필요 없다.
--
-- 닉네임(profiles.display_name)은 그런 제약이 없었으므로 여기서 추가한다.
--
-- 순서:
--   1) 기존 데이터에 이미 같은 닉네임이 있으면 유니크 인덱스 생성 자체가 실패하므로 먼저 정리한다.
--   2) display_name 에 대소문자 구분 없는 유니크 인덱스를 건다.
--   3) handle_new_user() 트리거가 기본으로 채우는 닉네임(구글 이름/이메일 앞부분)도 서로
--      겹칠 수 있다 — 겹치면 자동으로 접미사를 붙이도록 고친다. (실제 닉네임은 회원가입
--      화면에서 사용자가 정한 값으로 곧 다시 덮어쓰므로, 여기서는 "가입 자체가 막히는 것"만
--      방지하면 된다.)
--   4) 회원가입 화면이 계정을 만들기 전에 미리 중복 여부를 물어볼 수 있도록
--      is_nickname_taken() RPC 를 추가한다. 아직 로그인 전(anon)이라 RLS 로는 확인할 수
--      없어 security definer + anon 권한 부여로 처리한다 (부여하는 건 boolean 하나뿐,
--      profiles 원문을 열어주는 게 아니라 §14 "새 외부 의존성/노출 임의 추가" 취지를 벗어나지
--      않는다).
-- ============================================================================

-- 1) 기존 중복 정리 — 가장 먼저 만들어진 프로필은 그대로 두고, 나머지 중복에는 id 일부를 붙인다.
with duplicates as (
    select id,
           row_number() over (
               partition by lower(display_name)
               order by created_at asc
           ) as rn
    from public.profiles
    where display_name is not null
)
update public.profiles p
set display_name = p.display_name || '_' || substr(p.id::text, 1, 8)
from duplicates d
where p.id = d.id
  and d.rn > 1;

-- 2) 유니크 인덱스 (대소문자 구분 없음)
create unique index if not exists profiles_display_name_unique_idx
    on public.profiles (lower(display_name))
    where display_name is not null;

-- 3) 기본 닉네임 충돌도 안전하게 처리하도록 트리거 교체
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    base_name text;
begin
    base_name := coalesce(new.raw_user_meta_data ->> 'name', split_part(new.email, '@', 1));

    begin
        insert into public.profiles (id, display_name)
        values (new.id, base_name);
    exception when unique_violation then
        -- 기본값이 이미 쓰이고 있으면 뒤에 내 id 일부를 붙여 충돌을 피한다.
        -- 실제 닉네임은 회원가입 화면에서 곧 사용자가 정한 값으로 덮어써진다.
        insert into public.profiles (id, display_name)
        values (new.id, base_name || '_' || substr(new.id::text, 1, 8));
    end;

    return new;
end;
$$;

-- 4) 가입 전(비로그인) 상태에서도 중복 여부를 물어볼 수 있는 RPC
create or replace function public.is_nickname_taken(p_nickname text)
returns boolean
language sql
security definer
stable
as $$
    select exists (
        select 1 from public.profiles
        where lower(display_name) = lower(trim(p_nickname))
    );
$$;

grant execute on function public.is_nickname_taken(text) to anon, authenticated;
