# 검색 에이전트 프롬프트를 여기에 버전별로 둔다 (search_agent_v1.md ...).

현재 `chat` Edge Function이 로드하는 활성 버전은 `search_agent_v2.md` 다.
프롬프트 수정 시 파일을 덮어쓰지 말고 `_v3` 로 버전을 올린다 (CLAUDE.md §5.5) — 개선 이력이 남아야 한다.
버전을 올리면 `evals/` 케이스를 재실행하고 결과를 `evals/results/`에 커밋한다 (CLAUDE.md §11).
