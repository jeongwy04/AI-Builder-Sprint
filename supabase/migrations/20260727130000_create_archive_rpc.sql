-- ============================================================================
-- create_archive() — 그룹 생성 + 생성자 멤버십 등록을 한 트랜잭션으로.
--
-- 왜 필요한가:
--   archives의 SELECT 정책은 is_member(id)다. 앱에서 archives에 insert 직후
--   생성자는 아직 membership이 없어 방금 만든 행을 되읽지 못한다(RLS readback 함정).
--   또 insert(archive) → insert(membership)를 앱에서 두 번 하면 중간 실패 시
--   고아 archive가 남는다. security definer RPC로 원자화한다.
-- ============================================================================
create or replace function public.create_archive(
    p_name       text,
    p_group_type text default null
)
returns public.archives
language plpgsql
security definer
set search_path = public
as $$
declare
    v_archive public.archives;
begin
    insert into public.archives (name, group_type, created_by)
    values (p_name, p_group_type, auth.uid())
    returning * into v_archive;

    insert into public.memberships (archive_id, user_id)
    values (v_archive.id, auth.uid());

    return v_archive;
end;
$$;

revoke all on function public.create_archive(text, text) from public, anon;
grant execute on function public.create_archive(text, text) to authenticated;
