-- ============================================================================
-- match_memories 에 유사도 임계값 추가
--
-- 문제: 기존 함수는 거리순 상위 N건을 "무조건" 반환해, 관련 없는 기억도 결과가 적으면
--       딸려 나왔다("상관없는 것도 추천"). 코사인 거리(<=>) 임계값을 두어 충분히 가까운
--       기억만 반환한다. 미달이면 0건 → chat 함수/LLM 이 조건 완화를 제안한다.
--
-- 인자 시그니처가 바뀌므로(파라미터 추가) 기존 오버로드를 DROP 후 재생성한다.
-- security definer 는 붙이지 않는다 → 호출자 권한으로 실행되어 RLS 적용 (CLAUDE.md §8.3).
-- ============================================================================

drop function if exists public.match_memories(uuid, vector, date, date, int);

create or replace function public.match_memories(
    p_archive_id      uuid,
    p_query_embedding vector,
    p_date_from       date  default null,
    p_date_to         date  default null,
    p_limit           int   default 10,
    -- 코사인 거리 임계값(0=동일 … 2=정반대). 이 값보다 먼 기억은 제외한다.
    -- 실측 후 조정할 것. 값을 키우면 더 많이(느슨하게), 줄이면 더 엄격하게 걸러진다.
    p_max_distance    float default 0.5
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
      and (embedding <=> p_query_embedding) < p_max_distance
    order by embedding <=> p_query_embedding
    limit p_limit;
$$;
