#!/usr/bin/env bash
# ============================================================================
# 시드된 '삠삠' 기억 16개에 embed-memory 를 돌려 임베딩을 채운다.
#
# 사용법:
#   export SERVICE_ROLE_KEY="eyJ...(Supabase Settings→API→service_role)"
#   bash embed_bimbim.sh
#
# service_role 키로 호출해 RLS 를 우회한다(시드용 관리 작업). 이 키는 절대 앱/깃에 넣지 말 것.
# 각 응답의 "dim" 이 memories.embedding 차원(vector(4096))과 같아야 한다.
# 다르면 embed-memory 가 DB_ERROR 를 내니, 그때 컬럼 차원을 실제 dim 으로 맞춘다.
# ============================================================================
set -uo pipefail

SUPABASE_URL="https://oszjtpydseohbhldrpxd.supabase.co"
: "${SERVICE_ROLE_KEY:?SERVICE_ROLE_KEY 환경변수를 먼저 설정하세요 (Supabase Settings→API→service_role)}"

IDS=(
  bbbbbbbb-bbbb-bbbb-bbbb-000000000001
  bbbbbbbb-bbbb-bbbb-bbbb-000000000002
  bbbbbbbb-bbbb-bbbb-bbbb-000000000003
  bbbbbbbb-bbbb-bbbb-bbbb-000000000004
  bbbbbbbb-bbbb-bbbb-bbbb-000000000005
  bbbbbbbb-bbbb-bbbb-bbbb-000000000006
  bbbbbbbb-bbbb-bbbb-bbbb-000000000007
  bbbbbbbb-bbbb-bbbb-bbbb-000000000008
  bbbbbbbb-bbbb-bbbb-bbbb-000000000009
  bbbbbbbb-bbbb-bbbb-bbbb-000000000010
  bbbbbbbb-bbbb-bbbb-bbbb-000000000011
  bbbbbbbb-bbbb-bbbb-bbbb-000000000012
  bbbbbbbb-bbbb-bbbb-bbbb-000000000013
  bbbbbbbb-bbbb-bbbb-bbbb-000000000014
  bbbbbbbb-bbbb-bbbb-bbbb-000000000015
  bbbbbbbb-bbbb-bbbb-bbbb-000000000016
)

for id in "${IDS[@]}"; do
  printf '→ %s  ' "$id"
  curl -sS -X POST "$SUPABASE_URL/functions/v1/embed-memory" \
    -H "Authorization: Bearer $SERVICE_ROLE_KEY" \
    -H "apikey: $SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"memory_id\":\"$id\"}"
  printf '\n'
  sleep 0.3
done

echo "완료. 각 줄에 {\"ok\":true,\"embedded\":true,\"dim\":N} 이 보이면 성공이다."
