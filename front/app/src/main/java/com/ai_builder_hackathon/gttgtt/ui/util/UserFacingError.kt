package com.ai_builder_hackathon.gttgtt.ui.util

import io.github.jan.supabase.auth.exception.AuthRestException
import java.io.IOException

/**
 * 예외를 화면에 그대로 노출하지 않기 위한 변환기.
 *
 * supabase-kt / Ktor 예외의 [Throwable.message] 에는 요청 URL 과 헤더 전문이 들어 있다.
 * 여기엔 Authorization Bearer 토큰과 apikey 가 포함되므로 절대 UI 로 내보내면 안 된다.
 * (anon 키는 공개돼도 RLS 가 막아주지만, 사용자에게 보일 문장은 아니다.)
 *
 * 그래서 원문 message 는 **우리가 직접 만든 예외일 때만** 쓴다.
 * 그 외에는 종류별로 한국어 문장을 붙이고, 모르는 예외는 호출부의 [fallback] 으로 넘긴다.
 */
fun Throwable.toUserMessage(fallback: String): String = when (this) {
    // 레포지토리에서 사용자에게 보여주려고 직접 만든 예외 — 문구가 이미 한국어다.
    is IllegalStateException, is IllegalArgumentException, is NoSuchElementException ->
        message ?: fallback

    // AuthErrorCode 상수를 직접 참조하지 않고 이름으로 맞춘다.
    // supabase-kt 버전에 따라 enum 상수가 늘고 줄어서, 상수를 박아두면 업그레이드 때 컴파일이 깨진다.
    // 못 맞춘 코드는 그냥 fallback 으로 흘러가므로 안전하다.
    is AuthRestException -> when (errorCode?.name) {
        "InvalidCredentials" -> "이메일 또는 비밀번호가 맞지 않아요."
        "UserAlreadyExists" -> "이미 가입된 이메일이에요."
        "EmailNotConfirmed" -> "이메일 인증을 먼저 완료해주세요."
        "WeakPassword" -> "비밀번호가 너무 짧아요. 6자 이상으로 정해주세요."
        "OverEmailSendRateLimit", "OverRequestRateLimit" ->
            "요청이 너무 잦아요. 잠시 후 다시 시도해주세요."
        else -> fallback
    }

    // 네트워크 계층 — 기기가 오프라인이거나 서버에 못 닿는 경우.
    is IOException -> "네트워크 연결을 확인해주세요."

    else -> fallback
}
