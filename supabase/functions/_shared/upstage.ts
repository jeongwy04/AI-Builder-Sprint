// Upstage API 클라이언트 (OpenAI SDK 호환, baseURL 교체 방식).
// 키는 Edge Function secrets(UPSTAGE_API_KEY)에서만 읽는다. 앱에는 절대 없음.
// 모델 문자열은 환경변수로 주입한다 — 코드에 박지 않는다 (CLAUDE.md §9).

import { requireEnv } from "./http.ts";

const UPSTAGE_BASE_URL = "https://api.upstage.ai/v1";
const TIMEOUT_MS = 20_000; // CLAUDE.md §8.2 — 타임아웃 20초

// ---- 타입 -------------------------------------------------------------------

export interface ChatMessage {
  role: "system" | "user" | "assistant" | "tool";
  content: string | null;
  tool_calls?: ToolCall[];
  tool_call_id?: string;
}

export interface ToolCall {
  id: string;
  type: "function";
  function: { name: string; arguments: string };
}

export interface ToolSchema {
  type: "function";
  function: {
    name: string;
    description: string;
    parameters: Record<string, unknown>;
  };
}

interface ChatCompletionChoice {
  message: ChatMessage;
  finish_reason: string;
}

interface ChatCompletionResponse {
  choices: ChatCompletionChoice[];
  usage?: { prompt_tokens: number; completion_tokens: number };
}

interface EmbeddingResponse {
  data: { embedding: number[] }[];
}

// ---- 내부 유틸 --------------------------------------------------------------

function authHeaders(): HeadersInit {
  return {
    "Authorization": `Bearer ${requireEnv("UPSTAGE_API_KEY")}`,
    "Content-Type": "application/json",
  };
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(`${UPSTAGE_BASE_URL}${path}`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Upstage ${path} ${res.status}: ${text}`);
    }
    return await res.json() as T;
  } finally {
    clearTimeout(timer);
  }
}

// ---- 공개 API ---------------------------------------------------------------

/** 임베딩 1건 생성. model은 호출부에서 passage/query 구분해 넘긴다. */
export async function embed(input: string, model: string): Promise<number[]> {
  const started = Date.now();
  const json = await postJson<EmbeddingResponse>("/embeddings", { model, input });
  const vec = json.data[0]?.embedding;
  if (!vec || vec.length === 0) {
    throw new Error("Upstage embeddings: empty vector");
  }
  console.log(`[upstage.embed] model=${model} dim=${vec.length} ${Date.now() - started}ms`);
  return vec;
}

/** 대화 완성. function calling 지원. 로그로 모델/토큰/소요시간 남긴다 (CLAUDE.md §9). */
export async function chatCompletion(params: {
  model: string;
  messages: ChatMessage[];
  tools?: ToolSchema[];
  toolChoice?: "auto" | "none";
  temperature?: number;
}): Promise<ChatMessage> {
  const started = Date.now();
  const json = await postJson<ChatCompletionResponse>("/chat/completions", {
    model: params.model,
    messages: params.messages,
    tools: params.tools,
    tool_choice: params.tools ? (params.toolChoice ?? "auto") : undefined,
    temperature: params.temperature ?? 0.2,
  });
  const usage = json.usage;
  console.log(
    `[upstage.chat] model=${params.model} ` +
      `in=${usage?.prompt_tokens ?? "?"} out=${usage?.completion_tokens ?? "?"} ` +
      `${Date.now() - started}ms`,
  );
  const message = json.choices[0]?.message;
  if (!message) {
    throw new Error("Upstage chat: no choices returned");
  }
  return message;
}
