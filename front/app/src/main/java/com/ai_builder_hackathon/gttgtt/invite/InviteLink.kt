package com.ai_builder_hackathon.gttgtt.invite

import android.net.Uri

/**
 * 초대 딥링크 규약을 한 곳에 모은다.
 *
 * 형태: `traceapp://invite/<token>`
 * - 커스텀 스킴을 쓰는 이유: 배포 검증된 웹 도메인(assetlinks.json 호스팅)이 없어
 *   https App Links 를 자동 검증할 수 없다. 커스텀 스킴은 도메인 없이도 링크 클릭 →
 *   앱 열기가 동작한다. 도메인이 생기면 여기에 https intent-filter 를 추가하면 된다.
 * - 문자열을 화면·매니페스트 파서에서 각자 조립하지 않도록(오타 방지, CLAUDE.md §10)
 *   빌드·파싱을 모두 이 객체로 통일한다. 매니페스트의 scheme/host 도 아래 값과 반드시 일치해야 한다.
 */
object InviteLink {
    const val SCHEME = "traceapp"
    const val HOST = "invite"

    /** 공유용 딥링크 URL 을 만든다. */
    fun build(token: String): String = "$SCHEME://$HOST/$token"

    /**
     * 인텐트로 들어온 URI 에서 초대 토큰을 뽑는다. 우리 스킴/호스트가 아니면 null.
     * `traceapp://invite/<token>` 의 마지막 경로 세그먼트, 없으면 `?token=` 쿼리도 허용한다.
     */
    fun parseToken(uri: Uri?): String? {
        if (uri == null) return null
        if (uri.scheme != SCHEME || uri.host != HOST) return null
        val fromPath = uri.lastPathSegment?.trim()?.takeIf { it.isNotEmpty() }
        return fromPath ?: uri.getQueryParameter("token")?.trim()?.takeIf { it.isNotEmpty() }
    }
}
