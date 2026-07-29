package com.ai_builder_hackathon.gttgtt.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// design/redesign-mockup.html 의 :root CSS 변수를 그대로 옮긴 값이다.
// 눈대중으로 고치지 말고 시안 파일을 기준으로 갱신할 것.

// ── 감성 리디자인 팔레트 (2026-07) ──
// 톤: 차분한 스카이 블루(신뢰) + 따뜻한 크림 배경 + 차콜 텍스트 + 코랄 포인트.
// 이름은 유지하고 값만 교체해 전 화면 무드를 일괄 전환한다.

/** 메인 — 차분한 스카이 블루 (구 BrandGreen) */
val BrandGreen = Color(0xFF4C8DF6)

/** 진한 스카이 블루 — 텍스트/강조 */
val BrandGreenDark = Color(0xFF2F6FD8)

/** 연한 블루 배경 — 배지/틴트 */
val BrandGreenSoft = Color(0xFFEAF2FE)

/** 차콜 그레이 — 순수 검정 대신 부드럽게 */
val TextPrimary = Color(0xFF34373E)

/** 서브 텍스트 */
val TextSecondary = Color(0xFF8A8E97)

/** 더 약한 톤 */
val TextMuted = Color(0xFFB4B8C0)

/** 화면 배경 — 따뜻한 크림 (순백 아님) */
val ScreenBackground = Color(0xFFFAF7F2)

/** 카드/검색창 연한 배경 — 웜 라이트 */
val CardBackground = Color(0xFFF1EEE7)

/** 카드 표면 — 크림 배경 위에 뜨는 흰 '종이 사진' */
val SurfaceWhite = Color(0xFFFFFFFF)

/** 상단바 아이콘 — 차콜 */
val TopBarIcon = Color(0xFF34373E)

// ── 포인트 컬러 + 파스텔 틴트 ──
val AbodeBlue = Color(0xFF4C8DF6)
val AbodeYellow = Color(0xFFF3B24A)
val AbodeCoral = Color(0xFFFB7A6A)
val AbodeGreen = Color(0xFF37BE8C)
val AbodeBlueTint = Color(0xFFEAF2FE)
val AbodeYellowTint = Color(0xFFFCF3E2)
val AbodeCoralTint = Color(0xFFFFEDE9)
val AbodeGreenTint = Color(0xFFE7F6EF)
/** 카드 소프트 섀도우 — 차콜 저투명 (토스풍 도톰한 그림자) */
val CardShadow = Color(0x1A3A3D44)

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
        0f to Color(0xFF34C48F),
        0.55f to Color(0xFF17A67C),
        1f to Color(0xFF0C6E52),
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
