// ============================================================================
// [웹 배포용 · 단일 파일] embed-memory
// Supabase 대시보드 → Edge Functions → Create function → 이름 "embed-memory"
// → 이 파일 전체를 index.ts 에 붙여넣고 Deploy.
//
// CLI 버전(supabase/functions/embed-memory/index.ts)과 동작 동일.
// 웹 에디터는 ../_shared 공용 폴더를 못 쓰므로 공용 코드를 이 파일에 인라인함.
// ============================================================================

import { createClient, SupabaseClient } from "npm:@supabase/supabase-js@2";

// ---- 공용: CORS / 응답 / 환경변수 ------------------------------------------
const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
function errorResponse(code: string, message: string, status = 400): Response {
  return jsonResponse({ error: { code, message } }, status);
}
function requireEnv(name: string): string {
  const v = Deno.env.get(name);
  if (!v) throw new Error(`Missing required environment variable: ${name}`);
  return v;
}

// ---- 공용: Upstage 임베딩 ---------------------------------------------------
const UPSTAGE_BASE_URL = "https://api.upstage.ai/v1";
const TIMEOUT_MS = 20_000;

async function embed(input: string, model: string): Promise<number[]> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(`${UPSTAGE_BASE_URL}/embeddings`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${requireEnv("UPSTAGE_API_KEY")}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ model, input }),
      signal: controller.signal,
    });
    if (!res.ok) throw new Error(`Upstage /embeddings ${res.status}: ${await res.text()}`);
    const json = await res.json() as { data: { embedding: number[] }[] };
    const vec = json.data[0]?.embedding;
    if (!vec || vec.length === 0) throw new Error("Upstage embeddings: empty vector");
    console.log(`[embed] model=${model} dim=${vec.length}`);
    return vec;
  } finally {
    clearTimeout(timer);
  }
}

// ---- 공용: 요청자 컨텍스트 Supabase 클라이언트 ------------------------------
function createUserClient(req: Request): SupabaseClient {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) throw new Error("Missing Authorization header");
  return createClient(
    requireEnv("SUPABASE_URL"),
    requireEnv("SUPABASE_ANON_KEY"),
    { global: { headers: { Authorization: authHeader } } },
  );
}

// ---- 본체 -------------------------------------------------------------------
interface EmbedRequest {
  memory_id: string;
}
interface MemoryRow {
  id: string;
  memory_date: string;
  place_name: string | null;
}
interface NoteRow {
  body: string;
}

function dateExpression(isoDate: string): string {
  const [year, month] = isoDate.split("-");
  return `${year}년 ${Number(month)}월`;
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return errorResponse("METHOD_NOT_ALLOWED", "POST만 허용됩니다.", 405);

  let payload: EmbedRequest;
  try {
    payload = await req.json() as EmbedRequest;
  } catch {
    return errorResponse("BAD_REQUEST", "JSON 본문을 파싱할 수 없습니다.");
  }
  if (!payload.memory_id) return errorResponse("BAD_REQUEST", "memory_id가 필요합니다.");

  try {
    const supabase = createUserClient(req);

    const { data: memory, error: memErr } = await supabase
      .from("memories")
      .select("id, memory_date, place_name")
      .eq("id", payload.memory_id)
      .single<MemoryRow>();
    if (memErr || !memory) return errorResponse("MEMORY_NOT_FOUND", "기억을 찾을 수 없습니다.", 404);

    const { data: notes, error: noteErr } = await supabase
      .from("notes")
      .select("body")
      .eq("memory_id", payload.memory_id)
      .order("created_at", { ascending: true });
    if (noteErr) return errorResponse("DB_ERROR", noteErr.message, 500);

    const noteBodies = (notes as NoteRow[] ?? []).map((n) => n.body.trim());
    const parts = [...noteBodies, memory.place_name ?? "", dateExpression(memory.memory_date)]
      .filter((p) => p.length > 0);
    const searchText = parts.join(" \n");

    if (noteBodies.length === 0) {
      const { error: clearErr } = await supabase
        .from("memories")
        .update({ search_text: null, embedding: null })
        .eq("id", memory.id);
      if (clearErr) return errorResponse("DB_ERROR", clearErr.message, 500);
      return jsonResponse({ ok: true, embedded: false, reason: "no_notes" });
    }

    const vector = await embed(searchText, requireEnv("EMBED_PASSAGE_MODEL"));

    const { error: updErr } = await supabase
      .from("memories")
      .update({ search_text: searchText, embedding: `[${vector.join(",")}]` })
      .eq("id", memory.id);
    if (updErr) return errorResponse("DB_ERROR", updErr.message, 500);

    return jsonResponse({ ok: true, embedded: true, dim: vector.length });
  } catch (err) {
    console.error("[embed-memory]", err);
    return errorResponse("INTERNAL", "임베딩 처리 중 오류가 발생했습니다.", 500);
  }
});
