# ============================================================================
# 시드된 '삠삠' 기억 16개에 embed-memory 를 돌려 임베딩을 채운다 (PowerShell 버전).
#
# 사용법 (PowerShell):
#   $env:SERVICE_ROLE_KEY = "eyJ...(Supabase Settings→API→service_role)"
#   powershell -ExecutionPolicy Bypass -File supabase\seed\embed_bimbim.ps1
#
# service_role 키로 호출해 RLS 를 우회한다(시드용 관리 작업). 앱/깃엔 절대 넣지 말 것.
# 각 응답의 "dim" 이 memories.embedding 차원(vector(4096))과 같아야 한다.
# ============================================================================

$SupabaseUrl = "https://oszjtpydseohbhldrpxd.supabase.co"

if (-not $env:SERVICE_ROLE_KEY) {
    Write-Error "SERVICE_ROLE_KEY 환경변수를 먼저 설정하세요. 예: `$env:SERVICE_ROLE_KEY = 'eyJ...'"
    exit 1
}

$ids = @(
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000001",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000002",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000003",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000004",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000005",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000006",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000007",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000008",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000009",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000010",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000011",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000012",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000013",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000014",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000015",
    "bbbbbbbb-bbbb-bbbb-bbbb-000000000016"
)

$headers = @{
    Authorization = "Bearer $($env:SERVICE_ROLE_KEY)"
    apikey        = $env:SERVICE_ROLE_KEY
}

foreach ($id in $ids) {
    $body = @{ memory_id = $id } | ConvertTo-Json
    try {
        $resp = Invoke-RestMethod -Method Post `
            -Uri "$SupabaseUrl/functions/v1/embed-memory" `
            -Headers $headers -ContentType "application/json" -Body $body
        Write-Host "$id -> $($resp | ConvertTo-Json -Compress)"
    }
    catch {
        # 에러 응답의 본문(JSON)까지 찍어 원인을 구분한다.
        $errBody = $_.ErrorDetails.Message
        if (-not $errBody -and $_.Exception.Response) {
            try {
                $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $errBody = $sr.ReadToEnd()
            } catch {}
        }
        Write-Host "$id -> ERROR $($_.Exception.Message) | BODY: $errBody"
    }
    Start-Sleep -Milliseconds 300
}

Write-Host "완료. 각 줄에 embedded=true, dim=N 이 보이면 성공."
