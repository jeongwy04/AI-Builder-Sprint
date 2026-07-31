-- ============================================================================
-- match_memories() 에 유사도 임계값(p_max_distance) 추가
--
-- 문제: 기존 함수는 조건에 맞는 기억이 그룹에 6개 이상 있으면(SEARCH_LIMIT=6,
-- supabase/functions/chat/index.ts) 실제로 얼마나 관련 있는지와 무관하게 항상
-- "가장 가까운 6개"를 다 채워서 돌려줬다. 진짜 관련 있는 기억이 1~2개뿐이어도
-- 나머지 4~5개(사실상 무관한 기억)까지 검색 결과처럼 섞여 나온 원인이다.
--
-- CLAUDE.md §6.3: "결과가 0건이면 지어내지 말고 조건 완화를 제안한다" — 즉 0건이
-- 정상적으로 나올 수 있어야 하는데, 임계값이 없어 사실상 0건이 될 수 없었다.
--
-- p_max_distance 는 pgvector `<=>` 코사인 거리(0에 가까울수록 유사, 값이 클수록
-- 무관함)의 상한이다. 기본값 0.6은 시작점이다 — Embed 2 임베딩의 실제 분포를
-- 보고 튜닝이 필요할 수 있다:
--   - 관련 없는 기억이 계속 섞여 나오면 → 값을 낮춘다 (예: 0.5, 0.45)
--   - 분명 관련 있는데 0건으로 나오면 → 값을 높인다 (예: 0.65, 0.7)
-- 튜닝은 이 마이그레이션을 다시 실행하지 않고, 아래 CREATE OR REPLACE 를
-- SQL Editor에서 default 값만 바꿔 재실행하면 된다 (Edge Function 재배포 불필요 —
-- named parameter 기본값이라 하위 호환).
-- ============================================================================
create or replace function public.match_memories(
    p_archive_id     uuid,
    p_query_embedding vector,
    p_date_from      date default null,
    p_date_to        date default null,
    p_limit          int  default 10,
    p_max_distance   double precision default 0.6
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
      and embedding <=> p_query_embedding <= p_max_distance
    order by embedding <=> p_query_embedding
    limit p_limit;
$$;
