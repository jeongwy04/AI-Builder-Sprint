// CORS 헤더 — 모든 Edge Function이 공유한다.
// Android 클라이언트는 CORS 대상이 아니지만, 로컬 테스트(브라우저/Studio)와
// preflight 대응을 위해 표준 헤더를 둔다.

export const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

export function handlePreflight(req: Request): Response | null {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  return null;
}
