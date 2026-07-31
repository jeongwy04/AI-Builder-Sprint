// ============================================================================
// chat — Solar Pro 3 대화 + function calling → match_memories() → 결과 반환
//
// 흐름 (CLAUDE.md §8.1 / §9, HANDOFF F3):
//   사용자 질의 → Solar (도구 판단)
//     ├─ 조건 부족 → 되묻기 텍스트 그대로 반환
//     └─ search_memories 호출 → 질의 임베딩 → match_memories RPC
//            → 결과를 Solar에 되먹임 → 자연어 요약 + memory id 목록 반환
//   LLM 실패 시 → 키워드 폴백 검색 (빈 화면 금지, CLAUDE.md §9)
// ============================================================================

import { handlePreflight } from "../_shared/cors.ts";
import { errorResponse, jsonResponse, requireEnv } from "../_shared/http.ts";
import { createUserClient, SupabaseClient } from "../_shared/supabase.ts";
import {
  chatCompletion,
  type ChatMessage,
  embed,
  type ToolCall,
  type ToolSchema,
} from "../_shared/upstage.ts";

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
const SEARCH_LIMIT = 4;
// 코사인 거리 임계값 — 이보다 먼(=관련도 낮은) 기억은 반환하지 않는다. 실측 후 조정.
const MAX_DISTANCE = 0.5;

// ---- 프롬프트 로드 ----------------------------------------------------------
// 소스 오브 트루스는 prompts/search_agent_v2.md (버전 관리 대상).
// 배포 번들에서 파일을 못 읽으면 최소 인라인 프롬프트로 폴백해 항상 동작시킨다.

const PROMPT_URL = new URL(
  "../../../prompts/search_agent_v2.md",
  import.meta.url,
);

const FALLBACK_SYSTEM =
  "당신은 소그룹 공유 다이어리에서 추억을 함께 찾아 주는 대화 상대입니다. " +
  "장소·활동·사물·시기·사람·감정 중 단서가 하나라도 있으면(단어 하나여도) 되묻지 말고 " +
  "즉시 search_memories 도구를 호출합니다. 단서가 전혀 없을 때만 한 번 되묻습니다. " +
  "도구 결과 안에서만 답하고, 없는 기억을 지어내지 않습니다.";

const SEARCH_TOOL: ToolSchema = {
  type: "function",
  function: {
    name: "search_memories",
    description: "그룹의 기억을 의미 기반으로 검색한다. 사용자가 장소·활동·사물·시기·사람·감정 등 단서를 조금이라도 주면(단어 하나여도) 호출한다. 되묻기보다 검색을 우선한다.",
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

async function loadPrompt(): Promise<{ system: string; tool: ToolSchema }> {
  try {
    const md = await Deno.readTextFile(PROMPT_URL);
    const fence = md.match(/```json\s*([\s\S]*?)```/);
    const tool = fence ? JSON.parse(fence[1]) as ToolSchema : SEARCH_TOOL;
    // 시스템 프롬프트 = "## 시스템 프롬프트" 이후 ~ "## 도구 스키마" 이전
    const section = md.split("## 시스템 프롬프트")[1]?.split("## 도구 스키마")[0];
    const system = section?.trim() || FALLBACK_SYSTEM;
    return { system, tool };
  } catch {
    return { system: FALLBACK_SYSTEM, tool: SEARCH_TOOL };
  }
}

// ---- 검색 실행 --------------------------------------------------------------

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
    p_max_distance: MAX_DISTANCE,
  });
  if (error) throw new Error(`match_memories: ${error.message}`);
  return (data as MemoryHit[]) ?? [];
}

// 검색 결과 요약문을 서버에서 만든다(2차 LLM 호출 제거 → 지연/타임아웃 감소).
// 결과가 있으면 첫 기억의 메모 한 줄을 예시로 붙여 사람 말투로 짧게 요약한다.
function buildReply(hits: MemoryHit[]): string {
  if (hits.length === 0) {
    return "그 단서로는 관련된 기억을 찾지 못했어요. 시기나 장소를 조금 다르게 말해줄 수 있을까요?";
  }
  const snippet = hits[0].search_text
    ?.split("\n")
    .map((l) => l.trim())
    .find((l) => l.length > 0)
    ?.slice(0, 40);
  return snippet
    ? `이런 기억 ${hits.length}개를 찾았어요. 예를 들면 "${snippet}…" 같은 날이요.`
    : `관련된 기억 ${hits.length}개를 찾았어요.`;
}

// LLM 실패 시 키워드 폴백 (search_text ILIKE). 절대 빈 화면을 주지 않는다.
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

// ---- 세션 / 이력 ------------------------------------------------------------

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

async function loadHistory(
  supabase: SupabaseClient,
  sessionId: string,
): Promise<ChatMessage[]> {
  const { data } = await supabase
    .from("chat_messages")
    .select("role, content")
    .eq("session_id", sessionId)
    .in("role", ["user", "assistant"])
    .not("content", "is", null)
    .order("created_at", { ascending: false })
    .limit(HISTORY_LIMIT);
  const rows = (data as { role: string; content: string }[] ?? []).reverse();
  return rows.map((r) => ({
    role: r.role as ChatMessage["role"],
    content: r.content,
  }));
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

// ---- 핸들러 -----------------------------------------------------------------

Deno.serve(async (req: Request): Promise<Response> => {
  const preflight = handlePreflight(req);
  if (preflight) return preflight;
  if (req.method !== "POST") {
    return errorResponse("METHOD_NOT_ALLOWED", "POST만 허용됩니다.", 405);
  }

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

  await saveMessage(supabase, {
    session_id: sessionId,
    archive_id,
    role: "user",
    content: message,
  });

  try {
    const { system, tool } = await loadPrompt();
    const history = await loadHistory(supabase, sessionId);
    const messages: ChatMessage[] = [
      { role: "system", content: system },
      ...history,
      { role: "user", content: message },
    ];

    // 1차 호출 — Solar가 되물을지 검색할지 판단
    const first = await chatCompletion({
      model: requireEnv("SOLAR_MODEL"),
      messages,
      tools: [tool],
    });

    const toolCall: ToolCall | undefined = first.tool_calls?.[0];

    // (A) 도구 미호출 → 되묻기/일반 응답
    if (!toolCall) {
      const reply = first.content ?? "어떤 추억을 찾고 싶으신가요?";
      await saveMessage(supabase, {
        session_id: sessionId,
        archive_id,
        role: "assistant",
        content: reply,
      });
      return jsonResponse({ session_id: sessionId, reply, memory_ids: [] });
    }

    // (B) search_memories 호출
    const args = JSON.parse(toolCall.function.arguments) as SearchArgs;
    const hits = await runSearch(supabase, archive_id, args);
    const memoryIds = hits.map((h) => h.id);

    // 도구 호출 기록
    await saveMessage(supabase, {
      session_id: sessionId,
      archive_id,
      role: "assistant",
      content: null,
      tool_calls: first.tool_calls,
    });

    // 2차 LLM 요약 호출을 제거하고 요약문을 서버에서 만든다 —
    // Solar 왕복을 한 번 줄여 전체 지연을 크게 낮춘다(타임아웃 방지).
    const reply = buildReply(hits);

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
    // ---- 폴백: LLM/임베딩 실패 시 키워드 검색 ----
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
      return jsonResponse({
        session_id: sessionId,
        reply,
        memory_ids: memoryIds,
        degraded: true,
      });
    } catch (fbErr) {
      console.error("[chat] fallback failed:", fbErr);
      return errorResponse("INTERNAL", "검색 처리 중 오류가 발생했습니다.", 500);
    }
  }
});
