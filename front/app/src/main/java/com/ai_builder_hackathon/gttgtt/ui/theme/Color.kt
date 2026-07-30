package com.ai_builder_hackathon.gttgtt.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// design/redesign-mockup.html 의 :root CSS 변수를 그대로 옮긴 값이다.
// 눈대중으로 고치지 말고 시안 파일을 기준으로 갱신할 것.

// ── Figma 디자인 토큰 (Memory Collection App / theme.css 기준) ──
// 배경 베이지 + 파랑 + 화이트 카드 + 소프트 옐로/코랄 포인트. 값은 Figma 그대로.
// 이름은 유지하고 값만 교체해 전 화면을 일괄 전환한다.

/** primary — 파랑 (구 BrandGreen) */
val BrandGreen = Color(0xFF3B82F6)

/** 진한 파랑 — 강조 */
val BrandGreenDark = Color(0xFF2563EB)

/** 연한 파랑 배경 — 배지/틴트 */
val BrandGreenSoft = Color(0xFFE4EEFE)

/** foreground — 텍스트 (거의 검정, 살짝 부드럽게) */
val TextPrimary = Color(0xFF1A1A1A)

/** muted-foreground — 서브 텍스트 */
val TextSecondary = Color(0xFF8E8E93)

/** 더 약한 톤 */
val TextMuted = Color(0xFFB0B0B5)

/** background — 따뜻한 베이지 (다이얼로그 입력창·아바타 링 등 컴포넌트 배경으로도 쓰인다) */
val ScreenBackground = Color(0xFFEDEAE3)

/**
 * 화면 루트 배경 그라데이션 — 상단 옅은 하늘빛에서 하단 순백으로.
 * 흰 카드가 배경 위로 떠 보이도록 깊이감을 준다.
 * ⚠️ 화면의 최상위 컨테이너 배경에만 쓴다. 다이얼로그 입력창처럼 색 대비가 필요한 곳은
 * 계속 [ScreenBackground](불투명 색)를 쓴다 — 여기에 그라데이션을 넣으면 대비가 사라진다.
 */
val ScreenBackgroundBrush: Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color(0xFFEAF2FE),
        0.55f to Color(0xFFF7FAFF),
        1f to Color(0xFFFFFFFF),
    ),
)

/** muted / input-background — 검색창·입력창 배경 */
val CardBackground = Color(0xFFF5F2EC)

/** card — 흰 카드 */
val SurfaceWhite = Color(0xFFFFFFFF)

/** 상단바 아이콘 */
val TopBarIcon = Color(0xFF1A1A1A)

// ── 포인트 컬러 + 파스텔 틴트 (Figma) ──
val AbodeBlue = Color(0xFF3B82F6)
val AbodeYellow = Color(0xFFF5C33B)
val AbodeCoral = Color(0xFFFF6B6B)
val AbodeGreen = Color(0xFF37BE8C)
val AbodeBlueTint = Color(0xFFE4EEFE)
/** secondary — 소프트 옐로 */
val AbodeYellowTint = Color(0xFFFFF3BF)
val AbodeCoralTint = Color(0xFFFFECEC)
val AbodeGreenTint = Color(0xFFE7F6EF)
/** 카드/버튼 그림자 — 스티커처럼 떠 보이게 도톰하게 */
val CardShadow = Color(0x26000000)

// ── 글래스모피즘(다크 유리) ──
/** 다크 유리 표면 틴트 — 반투명이라 뒤 배경이 블러되어 비친다. */
val DarkGlassTint = Color(0xFF1E1E24).copy(alpha = 0.55f)
/** 유리 테두리(얇은 흰 하이라이트). */
val DarkGlassBorder = Color.White.copy(alpha = 0.14f)
/** 선택된 칩 상단 광원 느낌 테두리. */
val GlassTopHighlight = Color.White.copy(alpha = 0.15f)
/** 액션 카드 좌상단에서 번지는 파란 빛. */
val GlassBlueGlow = Color(0xFF3B82F6)
/** 다크 유리 위의 원형 버튼(다크 그레이). */
val DarkCircleButton = Color(0xFF2A2A31)

/** .chip — 댓글 등 기본 칩 */
val ChipBackground = Color(0xFFF4F3F0)
val ChipText = Color(0xFF55535F)

/** .chip.like — 좋아요 칩 */
val LikeChipBackground = Color(0xFFFCEDEA)
val LikeChipText = Color(0xFFB64B39)

/** .pmore — 게시물 더보기 아이콘 */
val MoreIcon = Color(0xFFC2C0CB)

// ── 그룹 채팅 ──
/** 채팅 화면 배경. 목록 화면(--screen)보다 살짝 보라빛이 돈다. */
val ChatBackground = Color(0xFFF4F3F8)

/** .datediv — 날짜 구분 칩 */
val DateChipBackground = Color(0xFFEBE9F3)
val DateChipText = Color(0xFF736F85)

/** .mwrap b — 말풍선 위 보낸사람 이름 */
val SenderNameText = Color(0xFF6F6D7C)

/** .mtime — 말풍선 옆 시각 */
val MessageTimeText = Color(0xFFB8B6C1)

/** .inbar .ico — 입력바의 사진·이모지 아이콘 */
val InputBarIcon = Color(0xFFB7B5C0)

// ── 추억 상세 ──
/** .hero-photo .count — 사진 위에 얹는 "1 / 8" 배지 */
val PhotoCountBackground = Color(0x8C14101E)

/** .dbody — 본문 */
val DetailBodyText = Color(0xFF57545F)

/** .people .p span — 참여자 이름 */
val ParticipantNameText = Color(0xFF6F6D7C)

/** .cmt p — 댓글 본문 */
val CommentText = Color(0xFF3D3B46)

/** .cmt .cf — 댓글 하단 메타 */
val CommentMetaText = Color(0xFFB0AEBB)

// ── 마이페이지 ──
/** .mp-head — 민트에서 연보라로 넘어가는 헤더 배경 (160deg) */
val MyPageHeaderGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color(0xFFDFF3EA),
        0.6f to Color(0xFFEAF6F0),
        1f to Color(0xFFF4F1FA),
    )
)

/** .mp-set — 헤더 우상단 설정 아이콘 */
val MyPageSettingsIcon = Color(0xFF4A6D61)

/** .mp-sub — 상태 문구 */
val MyPageSubText = Color(0xFF5F7A70)

// .statcard .si — 통계 카드 아이콘 칩. 배경/전경 짝으로 쓴다.
val StatMemoryBackground = Color(0xFFE3F4EE)
val StatMediaBackground = Color(0xFFFFF1DC)
val StatMediaIcon = Color(0xFF8A5A0F)

/** .menucard a — 메뉴 항목 */
val MenuItemText = Color(0xFF33313C)
val MenuItemIcon = Color(0xFF7C7A88)
val MenuDivider = Color(0xFFF1EFEC)

/** .cv — 우측 꺾쇠 */
val ChevronIcon = Color(0xFFC9C7D2)

/** AI 추억 찾기 FAB 그라디언트 (140deg) */
val AiFabGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color(0xFF60A5FA),
        0.55f to Color(0xFF3B82F6),
        1f to Color(0xFF1D4ED8),
    )
)

// ── 그룹 썸네일 그라디언트 (.beach / .forest / ... , CSS 155deg) ──
// CSS 의 155deg 는 위에서 아래로 살짝 오른쪽. Compose 기본 linearGradient(모서리→모서리)로 근사한다.

private fun verticalish(vararg stops: Pair<Float, Color>): Brush =
    Brush.linearGradient(colorStops = stops)

val BeachGradient = verticalish(
    0f to Color(0xFFFFD79B),
    0.42f to Color(0xFFFF9F77),
    0.72f to Color(0xFFC76FA0),
    1f to Color(0xFF6F58BD),
)

val ForestGradient = verticalish(
    0f to Color(0xFFB3D9A1),
    0.55f to Color(0xFF63A06F),
    1f to Color(0xFF2F6B4D),
)

val FoodGradient = verticalish(
    0f to Color(0xFFECB56E),
    0.55f to Color(0xFFCD7A31),
    1f to Color(0xFF804321),
)

val LaptopGradient = verticalish(
    0f to Color(0xFFD3D8DE),
    0.55f to Color(0xFF99A2AD),
    1f to Color(0xFF5B6470),
)

val FamilyGradient = verticalish(
    0f to Color(0xFFF6CDA9),
    0.55f to Color(0xFFE39C86),
    1f to Color(0xFFB06A79),
)

val SeaGradient = verticalish(
    0f to Color(0xFFC3E6F0),
    0.55f to Color(0xFF6FC0D6),
    1f to Color(0xFF2F8CB0),
)

val NightGradient = verticalish(
    0f to Color(0xFF8F7FC9),
    0.55f to Color(0xFF5B4AA0),
    1f to Color(0xFF2C2154),
)

// ── 멤버 아바타 그라디언트 (.a1 ~ .a5, CSS 135deg) ──
// 사용자 id 해시로 이 안에서 하나를 고른다.
val AvatarGradients: List<Brush> = listOf(
    Brush.linearGradient(listOf(Color(0xFFFFB37A), Color(0xFFE8637F))),
    Brush.linearGradient(listOf(Color(0xFF7FD1B0), Color(0xFF2F9E86))),
    Brush.linearGradient(listOf(Color(0xFF9CC4F0), Color(0xFF5F7ED8))),
    Brush.linearGradient(listOf(Color(0xFFF0A6C6), Color(0xFFC86A9E))),
    Brush.linearGradient(listOf(Color(0xFFF6CF7A), Color(0xFFE0A24A))),
)
