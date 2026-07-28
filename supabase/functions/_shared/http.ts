// 공통 응답 헬퍼 — 에러 응답 형식을 한 곳에서 고정한다 (CLAUDE.md §8.2).
//   { "error": { "code": "...", "message": "..." } }

import { corsHeaders } from "./cors.ts";

export interface ApiError {
  error: { code: string; message: string };
}

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

export function errorResponse(
  code: string,
  message: string,
  status = 400,
): Response {
  const body: ApiError = { error: { code, message } };
  return jsonResponse(body, status);
}

// 누락 시 즉시 실패 (CLAUDE.md §10 — 환경변수는 없으면 바로 실패시킨다).
export function requireEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}
