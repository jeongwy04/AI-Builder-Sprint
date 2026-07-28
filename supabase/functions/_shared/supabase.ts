// 요청자 컨텍스트를 유지하는 Supabase 클라이언트 생성.
// 클라이언트의 Authorization 헤더를 그대로 전달해 RLS가 사용자 권한으로 적용된다
// (CLAUDE.md §8.2). service_role 키는 쓰지 않는다.

import { createClient, SupabaseClient } from "npm:@supabase/supabase-js@2";
import { requireEnv } from "./http.ts";

export function createUserClient(req: Request): SupabaseClient {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    throw new Error("Missing Authorization header");
  }
  return createClient(
    requireEnv("SUPABASE_URL"),
    requireEnv("SUPABASE_ANON_KEY"),
    { global: { headers: { Authorization: authHeader } } },
  );
}

export type { SupabaseClient };
