// ============================================================================
// [웹 배포용 · 단일 파일] chat
// Supabase 대시보드 → Edge Functions → Create function → 이름 "chat"
// → 이 파일 전체를 index.ts 에 붙여넣고 Deploy.
//
// CLI 버전(supabase/functions/chat/index.ts)과 동작 동일.
// 웹 배포에서는 prompts/ 파일을 못 읽으므로 시스템 프롬프트를 이 파일에 인라인함.
// (버전 관리 원본은 prompts/search_agent_v1.md 에 계속 유지)
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

// ---- 공용: Upstage 타입 & 호출 ---------------------------------------------
const UPSTAGE_BASE_URL = "https://api.upstage.ai/v1";
const TIMEOUT_MS = 20_000;

interface ChatMessage {
  role: "system" | "user" | "assistant" | "tool";
  content: string | null;
  tool_calls?: ToolCall[];
  tool_call_id?: string;
}
interface ToolCall {
  id: string;
  type: "function";
  function: { name: string; arguments: string };
}
interface ToolSchema {
  type: "function";
  function: { name: string; description: string; parameters: Record<string, unknown> };
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(`${UPSTAGE_BASE_URL}${path}`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${requireEnv("UPSTAGE_API_KEY")}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    if (!res.ok) throw new Error(`Upstage ${path} ${res.status}: ${await res.text()}`);
    return await res.json() as T;
  } finally {
    clearTimeout(timer);
  }
}

async function embed(input: string, model: string): Promise<number[]> {
  const json = await postJson<{ data: { embedding: number[] }[] }>("/embeddings", { model, input });
  const vec = json.data[0]?.embedding;
  if (!vec || vec.length === 0) throw new Error("Upstage embeddings: empty vector");
  console.log(`[embed] model=${model} dim=${vec.length}`);
  return vec;
}

async function chatCompletion(params: {
  model: string;
  messages: ChatMessage[];
  tools?: ToolSchema[];
  toolChoice?: "auto" | "none";
  temperature?: number;
}): Promise<ChatMessage> {
  const started = Date.now();
  const json = await postJson<{
    choices: { message: ChatMessage }[];
    usage?: { prompt_tokens: number; completion_tokens: number };
  }>("/chat/completions", {
    model: params.model,
    messages: params.messages,
    tools: params.tools,
    tool_choice: params.tools ? (params.toolChoice ?? "auto") : undefined,
    temperature: params.temperature ?? 0.2,
  });
  console.log(
    `[chat] model=${params.model} in=${json.usage?.prompt_tokens ?? "?"} ` +
      `out=${json.usage?.completion_tokens ?? "?"} ${Date.now() - started}ms`,
  );
  const message = json.choices[0]?.message;
  if (!message) throw new Error("Upstage chat: no choices returned");
  return message;
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

// ---- 프롬프트 (prompts/search_agent_v1.md 인라인) ---------------------------
const SYSTEM_PROMPT =
  "당신은 소그룹의 공유 다이어리에서 추억을 함께 찾아 주는 대화 상대입니다. " +
  "사용자는 검색창이 아니라 당신과의 대화로 기억을 꺼냅니다.\n" +
  "1. 되묻기 우선: 조건(시기·장소·인물·상황)이 부족하면 즉시 검색하지 말고 가장 도움이 될 한 가지만 되묻습니다.\n" +
  "2. 조건이 충분하면 search_memories 도구를 호출합니다. 사용자 표현을 그대로 semantic_query에 담고, 날짜가 특정되면 date_from/date_to로 좁힙니다.\n" +
  "3. 도구 결과 안에서만 답합니다. 없는 기억·날짜·장소·인물을 지어내지 않습니다. 어떤 기억을 찾았는지 사람의 말투로 짧게 요약합니다.\n" +
  "4. 결과가 0건이면 지어내지 말고 조건 완화나 다른 단서를 제안합니다.\n" +
  "5. 이야기(메모) 없는 사진이 관련돼 보이면 그 날의 이야기를 물어 메모로 남기도록 유도합니다.\n" +
  "6. 따뜻하고 담백하게. 사진의 픽셀이 아니라 사람이 남긴 문장을 근거로 찾습니다.";

const SEARCH_TOOL: ToolSchema = {
  type: "function",
  function: {
    name: "search_memories",
    description: "그룹의 기억을 의미 기반으로 검색한다. 조건이 충분할 때만 호출한다.",
    parameters: {
      type: "object",
      properties: {
        semantic_query: { type: "string" },
        date_from: { type: "string" },
        date_to: { type: "string" },
        place: { type: "string" },
        people: { type: "array", items: { type: "string" } },
      },
      required: ["semantic_query"],
    },
  },
};

// ---- 본체 -------------------------------------------------------------------
interface ChatRequest {
  archive_id: string;
  message: string;
  session_id?: string;
}
interface MemoryHit {
  id: string;
  memory_date: string;
  place_name: string | null;
  search_text: string | null;
}
interface SearchArgs {
  semantic_query: string;
  date_from?: string;
  date_to?: string;
  place?: string;
  people?: string[];
}

const HISTORY_LIMIT = 8;
const SEARCH_LIMIT = 6;

async function runSearch(
  supabase: SupabaseClient,
  archiveId: string,
  args: SearchArgs,
): Promise<MemoryHit[]> {
  const embedText = [args.semantic_query, args.place, (args.people ?? []).join(" ")]
    .filter((s) => s && s.length > 0)
    .join(" ");
  const queryVec = await embed(embedText, requireEnv("EMBED_QUERY_MODEL"));
  const { data, error } = await supabase.rpc("match_memories", {
    p_archive_id: archiveId,
    p_query_embedding: `[${queryVec.join(",")}]`,
    p_date_from: args.date_from ?? null,
    p_date_to: args.date_to ?? null,
    p_limit: SEARCH_LIMIT,
  });
  if (error) throw new Error(`match_memories: ${error.message}`);
  return (data as MemoryHit[]) ?? [];
}

async function keywordFallback(
  supabase: SupabaseClient,
  archiveId: string,
  message: string,
): Promise<MemoryHit[]> {
  const { data } = await supabase
    .from("memories")
    .select("id, memory_date, place_name, search_text")
    .eq("archive_id", archiveId)
    .ilike("search_text", `%${message}%`)
    .order("memory_date", { ascending: false })
    .limit(SEARCH_LIMIT);
  return (data as MemoryHit[]) ?? [];
}

async function ensureSession(
  supabase: SupabaseClient,
  archiveId: string,
  sessionId?: string,
): Promise<string> {
  if (sessionId) return sessionId;
  const { data, error } = await supabase
    .from("chat_sessions")
    .insert({ archive_id: archiveId })
    .select("id")
    .single<{ id: string }>();
  if (error || !data) throw new Error(`create session: ${error?.message}`);
  return data.id;
}

async function loadHistory(supabase: SupabaseClient, sessionId: string): Promise<ChatMessage[]> {
  const { data } = await supabase
    .from("chat_messages")
    .select("role, content")
    .eq("session_id", sessionId)
    .in("role", ["user", "assistant"])
    .not("content", "is", null)
    .order("created_at", { ascending: false })
    .limit(HISTORY_LIMIT);
  const rows = (data as { role: string; content: string }[] ?? []).reverse();
  return rows.map((r) => ({ role: r.role as ChatMessage["role"], content: r.content }));
}

async function saveMessage(
  supabase: SupabaseClient,
  row: {
    session_id: string;
    archive_id: string;
    role: string;
    content: string | null;
    tool_calls?: unknown;
    result_memory_ids?: string[];
  },
): Promise<void> {
  const { error } = await supabase.from("chat_messages").insert(row);
  if (error) console.error("[chat] saveMessage:", error.message);
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return errorResponse("METHOD_NOT_ALLOWED", "POST만 허용됩니다.", 405);

  let payload: ChatRequest;
  try {
    payload = await req.json() as ChatRequest;
  } catch {
    return errorResponse("BAD_REQUEST", "JSON 본문을 파싱할 수 없습니다.");
  }
  if (!payload.archive_id || !payload.message) {
    return errorResponse("BAD_REQUEST", "archive_id와 message가 필요합니다.");
  }

  const supabase = createUserClient(req);
  const { archive_id, message } = payload;

  let sessionId: string;
  try {
    sessionId = await ensureSession(supabase, archive_id, payload.session_id);
  } catch (err) {
    console.error("[chat] session", err);
    return errorResponse("DB_ERROR", "세션을 만들 수 없습니다.", 500);
  }

  await saveMessage(supabase, { session_id: sessionId, archive_id, role: "user", content: message });

  try {
    const history = await loadHistory(supabase, sessionId);
    const messages: ChatMessage[] = [
      { role: "system", content: SYSTEM_PROMPT },
      ...history,
      { role: "user", content: message },
    ];

    const first = await chatCompletion({
      model: requireEnv("SOLAR_MODEL"),
      messages,
      tools: [SEARCH_TOOL],
    });
    const toolCall: ToolCall | undefined = first.tool_calls?.[0];

    // (A) 도구 미호출 → 되묻기/일반 응답
    if (!toolCall) {
      const reply = first.content ?? "어떤 추억을 찾고 싶으신가요?";
      await saveMessage(supabase, { session_id: sessionId, archive_id, role: "assistant", content: reply });
      return jsonResponse({ session_id: sessionId, reply, memory_ids: [] });
    }

    // (B) search_memories 호출
    const args = JSON.parse(toolCall.function.arguments) as SearchArgs;
    const hits = await runSearch(supabase, archive_id, args);

    await saveMessage(supabase, {
      session_id: sessionId,
      archive_id,
      role: "assistant",
      content: null,
      tool_calls: first.tool_calls,
    });

    const toolResult = hits.map((h) => ({
      id: h.id,
      date: h.memory_date,
      place: h.place_name,
      note: h.search_text?.slice(0, 200) ?? null,
    }));

    const second = await chatCompletion({
      model: requireEnv("SOLAR_MODEL"),
      messages: [
        ...messages,
        { role: "assistant", content: null, tool_calls: first.tool_calls },
        {
          role: "tool",
          tool_call_id: toolCall.id,
          content: JSON.stringify({ count: hits.length, memories: toolResult }),
        },
      ],
      toolChoice: "none",
    });

    const reply = second.content ??
      (hits.length > 0 ? `${hits.length}장 찾았어요.` : "관련된 기억을 찾지 못했어요.");
    const memoryIds = hits.map((h) => h.id);

    await saveMessage(supabase, {
      session_id: sessionId,
      archive_id,
      role: "tool",
      content: JSON.stringify({ count: hits.length }),
      result_memory_ids: memoryIds,
    });
    await saveMessage(supabase, {
      session_id: sessionId,
      archive_id,
      role: "assistant",
      content: reply,
      result_memory_ids: memoryIds,
    });

    return jsonResponse({ session_id: sessionId, reply, memory_ids: memoryIds });
  } catch (err) {
    // ---- 폴백: LLM/임베딩 실패 시 키워드 검색 (빈 화면 금지) ----
    console.error("[chat] fallback:", err);
    try {
      const hits = await keywordFallback(supabase, archive_id, message);
      const reply = hits.length > 0
        ? `대화 검색이 잠시 불안정해서 키워드로 ${hits.length}장 찾았어요.`
        : "지금은 검색이 어려워요. 잠시 후 다시 시도해 주세요.";
      const memoryIds = hits.map((h) => h.id);
      await saveMessage(supabase, {
        session_id: sessionId,
        archive_id,
        role: "assistant",
        content: reply,
        result_memory_ids: memoryIds,
      });
      return jsonResponse({ session_id: sessionId, reply, memory_ids: memoryIds, degraded: true });
    } catch (fbErr) {
      console.error("[chat] fallback failed:", fbErr);
      return errorResponse("INTERNAL", "검색 처리 중 오류가 발생했습니다.", 500);
    }
  }
});
