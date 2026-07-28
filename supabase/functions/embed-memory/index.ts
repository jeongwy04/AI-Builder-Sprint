// ============================================================================
// embed-memory — search_text 조립 → Embed(passage) 임베딩 → memories 갱신
//
// 트리거: 메모(note) 추가·수정·삭제 시 앱이 이 함수를 호출한다 (CLAUDE.md §5.6).
// search_text 조립 로직은 "이 파일 한 곳"에만 둔다. 앱에서 조립하지 않는다.
// ============================================================================

import { handlePreflight } from "../_shared/cors.ts";
import { errorResponse, jsonResponse, requireEnv } from "../_shared/http.ts";
import { createUserClient } from "../_shared/supabase.ts";
import { embed } from "../_shared/upstage.ts";

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

// 날짜를 검색에 도움이 되는 자연어 표현으로 바꾼다 ("2025-12" → "2025년 12월").
// 사용자는 "작년 겨울"처럼 질의하므로 연·월 텍스트를 임베딩에 함께 녹인다.
function dateExpression(isoDate: string): string {
  const [year, month] = isoDate.split("-");
  return `${year}년 ${Number(month)}월`;
}

Deno.serve(async (req: Request): Promise<Response> => {
  const preflight = handlePreflight(req);
  if (preflight) return preflight;

  if (req.method !== "POST") {
    return errorResponse("METHOD_NOT_ALLOWED", "POST만 허용됩니다.", 405);
  }

  let payload: EmbedRequest;
  try {
    payload = await req.json() as EmbedRequest;
  } catch {
    return errorResponse("BAD_REQUEST", "JSON 본문을 파싱할 수 없습니다.");
  }
  if (!payload.memory_id) {
    return errorResponse("BAD_REQUEST", "memory_id가 필요합니다.");
  }

  try {
    const supabase = createUserClient(req);

    // 1. 기억 메타 (RLS로 멤버만 조회 가능)
    const { data: memory, error: memErr } = await supabase
      .from("memories")
      .select("id, memory_date, place_name")
      .eq("id", payload.memory_id)
      .single<MemoryRow>();
    if (memErr || !memory) {
      return errorResponse("MEMORY_NOT_FOUND", "기억을 찾을 수 없습니다.", 404);
    }

    // 2. 이 기억의 모든 메모
    const { data: notes, error: noteErr } = await supabase
      .from("notes")
      .select("body")
      .eq("memory_id", payload.memory_id)
      .order("created_at", { ascending: true });
    if (noteErr) {
      return errorResponse("DB_ERROR", noteErr.message, 500);
    }

    // 3. search_text 조립 = 메모 전부 + 장소 + 날짜표현
    const noteBodies = (notes as NoteRow[] ?? []).map((n) => n.body.trim());
    const parts: string[] = [
      ...noteBodies,
      memory.place_name ?? "",
      dateExpression(memory.memory_date),
    ].filter((p) => p.length > 0);
    const searchText = parts.join(" \n");

    // 메모가 하나도 없으면 임베딩을 만들지 않는다.
    // (이야기 없는 사진은 검색 대상에서 제외 — 대화로 메모를 채우도록 유도)
    if (noteBodies.length === 0) {
      const { error: clearErr } = await supabase
        .from("memories")
        .update({ search_text: null, embedding: null })
        .eq("id", memory.id);
      if (clearErr) return errorResponse("DB_ERROR", clearErr.message, 500);
      return jsonResponse({ ok: true, embedded: false, reason: "no_notes" });
    }

    // 4. Embed(passage)로 임베딩
    const vector = await embed(searchText, requireEnv("EMBED_PASSAGE_MODEL"));

    // 5. memories 갱신 — search_text + embedding 항상 함께 (CLAUDE.md §5.6)
    // pgvector 컬럼은 PostgREST에 문자열 리터럴 "[v1,v2,...]"로 전달한다.
    const { error: updErr } = await supabase
      .from("memories")
      .update({ search_text: searchText, embedding: `[${vector.join(",")}]` })
      .eq("id", memory.id);
    if (updErr) {
      return errorResponse("DB_ERROR", updErr.message, 500);
    }

    return jsonResponse({ ok: true, embedded: true, dim: vector.length });
  } catch (err) {
    console.error("[embed-memory]", err);
    return errorResponse("INTERNAL", "임베딩 처리 중 오류가 발생했습니다.", 500);
  }
});
