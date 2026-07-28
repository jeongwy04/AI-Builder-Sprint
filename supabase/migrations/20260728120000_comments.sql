-- ============================================================================
-- comments — 게시물(memory)에 대한 댓글
--
-- CLAUDE.md §7 데이터 모델에 원래 있던 테이블이나 마이그레이션에 빠져 있어 추가한다.
-- notes 와 분리하는 이유:
--   notes 는 검색의 유일한 재료다(search_text → embedding, §5.6). 댓글 같은 잡담을
--   notes 로 넣으면 의미검색 벡터가 오염된다. 댓글은 검색에 관여하지 않는 별개 테이블로 둔다.
--
-- 테넌트 패턴: archive_id + is_member() RLS. (reactions 와 동일)
-- 권한: 멤버는 누구나 조회·작성 가능(§6.1). 수정·삭제는 작성자 본인만(소유권 — 역할 아님).
-- ============================================================================
create table public.comments (
    id         uuid primary key default gen_random_uuid(),
    memory_id  uuid not null references public.memories(id) on delete cascade,
    archive_id uuid not null references public.archives(id) on delete cascade,
    author_id  uuid not null references auth.users(id) default auth.uid(),
    body       text not null,
    created_at timestamptz not null default now()
);

create index comments_memory_idx on public.comments (memory_id, created_at);

alter table public.comments enable row level security;

create policy "comments_select" on public.comments
    for select using (public.is_member(archive_id));
create policy "comments_insert" on public.comments
    for insert to authenticated
    with check (public.is_member(archive_id) and author_id = auth.uid());
create policy "comments_update_own" on public.comments
    for update using (author_id = auth.uid());
create policy "comments_delete_own" on public.comments
    for delete using (author_id = auth.uid());
