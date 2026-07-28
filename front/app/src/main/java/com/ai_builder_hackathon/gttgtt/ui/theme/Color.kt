package com.ai_builder_hackathon.gttgtt.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// design/redesign-mockup.html 의 :root CSS 변수를 그대로 옮긴 값이다.
// 눈대중으로 고치지 말고 시안 파일을 기준으로 갱신할 것.

/** --brand */
val BrandGreen = Color(0xFF12A575)

/** --brand-d · 밝은 배경 위의 그린 텍스트 */
val BrandGreenDark = Color(0xFF0B6B4E)

/** --brand-soft · "+N" 배지처럼 연한 그린 배경 */
val BrandGreenSoft = Color(0xFFE1F3EC)

/** --ink */
val TextPrimary = Color(0xFF14141A)

/** --sub */
val TextSecondary = Color(0xFF9A9AA3)

/** .gtime · 부가 정보 중에서도 더 약한 톤 */
val TextMuted = Color(0xFFB6B4BF)

/** --screen · 화면 배경. 순백이 아니라 니어화이트다. */
val ScreenBackground = Color(0xFFF5F6F8)

/** --cardsoft · 카드/검색창 배경 */
val CardBackground = Color(0xFFECEDF1)

val SurfaceWhite = Color(0xFFFFFFFF)

/** .tbar .bk 아이콘 색 */
val TopBarIcon = Color(0xFF2A2836)

/** .pin — 고정 게시물 배지 */
val PinBackground = Color(0xFFFFF4DE)
val PinText = Color(0xFF8A5A0F)

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
