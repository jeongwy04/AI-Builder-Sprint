-- ============================================================================
-- 게시물(기억)을 그룹 채팅방에 공유하는 기능
--
-- 피드 카드의 "보내기" 버튼 → 이 그룹의 채팅(messages)에 그 기억을 참조하는
-- 메시지 한 건을 남긴다. 텍스트/사진 없이 기억 참조만 있는 메시지도 허용해야 하므로
-- messages 의 CHECK 제약을 넓힌다.
--
-- shared_memory_id 는 기억이 지워져도 메시지 자체(대화 맥락)는 남기고 싶어서
-- on delete set null 로 둔다 — cascade 로 하면 기억 하나 지울 때 채팅 기록까지 지워진다.
-- ============================================================================

-- 기존 CHECK 제약은 CREATE TABLE 에서 이름 없이 선언돼 있어 실제 생성된 이름을
-- 코드에서 확신할 수 없다. contype = 'c' 로 찾아서 안전하게 지운다.
do $$
declare
    con record;
begin
    for con in
        select conname from pg_constraint
        where conrelid = 'public.messages'::regclass
          and contype = 'c'
    loop
        execute format('alter table public.messages drop constraint %I', con.conname);
    end loop;
end $$;

alter table public.messages
    add column shared_memory_id uuid references public.memories(id) on delete set null;

alter table public.messages
    add constraint messages_content_check
    check (
        body is not null
        or image_path is not null
        or shared_memory_id is not null
    );

-- RLS 정책은 그대로 둔다 — messages_select/insert 가 이미 is_member(archive_id) 로
-- 걸려 있고, 공유되는 memories 행 자체도 memories 테이블의 RLS 로 별도 보호된다.
