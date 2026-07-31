-- ============================================================================
-- 프로필 아바타 전용 Storage 버킷
--
-- profiles 는 archive_id 가 없는 전역 테이블이라 (§7 데이터 모델) 'memories' 버킷의
-- is_member(archive_id) 격리 패턴을 그대로 쓸 수 없다 — 대신 폴더 1단계를 소유자
-- user_id 로 삼아 본인만 쓰기 허용한다.
--
-- 조회는 그룹 경계 없이 여러 보관소에 걸쳐 다른 멤버에게도 보여야 하므로, 이미
-- profiles 테이블 자체가 "profiles_select_authenticated" 로 로그인 사용자 전체에게
-- 열려 있는 것과 같은 경계로, 로그인 사용자면 누구나 읽을 수 있게 한다.
-- (CLAUDE.md §5.2 의 "public 버킷으로 전환하지 않는다"는 memories 버킷 한정 규칙이라
-- 별도 버킷으로 분리하되, 여기서도 public=false 를 유지하고 인증된 사용자로만 select 를
-- 제한한다 — 익명 접근은 여전히 막는다.)
--
-- 경로 규칙: {user_id}/{uuid}.{ext} → foldername[1] 이 user_id
-- ============================================================================
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', false)
on conflict (id) do nothing;

create policy "avatar_select_authenticated" on storage.objects
    for select to authenticated using (
        bucket_id = 'avatars'
    );
create policy "avatar_insert_owner" on storage.objects
    for insert to authenticated with check (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
create policy "avatar_update_owner" on storage.objects
    for update using (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
create policy "avatar_delete_owner" on storage.objects
    for delete using (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
