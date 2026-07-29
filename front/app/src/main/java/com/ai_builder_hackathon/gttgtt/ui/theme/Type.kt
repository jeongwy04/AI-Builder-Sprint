package com.ai_builder_hackathon.gttgtt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.R

/**
 * res/font/ 의 정적 굵기별 TTF를 그대로 매핑한다.
 *
 * 화면 대부분이 MaterialTheme.typography 를 직접 참조하지 않고
 * `Text(fontWeight = ...)` 처럼 굵기만 지정해서 쓴다 — 이 경우 fontFamily 는
 * 앰비언트 LocalTextStyle(= 아래 Typography.bodyLarge)에서 상속된다.
 * 그래서 여기 하나만 바꾸면 앱 전체 글꼴이 한 번에 Pretendard로 바뀐다.
 */
val PretendardFontFamily = FontFamily(
    Font(R.font.pretendard_thin, FontWeight.Thin),
    Font(R.font.pretendard_extralight, FontWeight.ExtraLight),
    Font(R.font.pretendard_light, FontWeight.Light),
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
    Font(R.font.pretendard_black, FontWeight.Black),
)

/**
 * 헤딩·큰 문구·숫자용 디스플레이 서체 = **Gmarket Sans** (둥근 헤비 톤).
 * "그때그때", 온보딩 헤딩, 마이페이지 이름/통계 숫자 등 큰 문구에 쓴다.
 * 화면에서 `fontFamily = DisplayFontFamily` 로 지정한다.
 */
val DisplayFontFamily = FontFamily(
    Font(R.font.gmarketsans_light, FontWeight.Light),
    Font(R.font.gmarketsans_medium, FontWeight.Normal),
    Font(R.font.gmarketsans_medium, FontWeight.Medium),
    Font(R.font.gmarketsans_bold, FontWeight.SemiBold),
    Font(R.font.gmarketsans_bold, FontWeight.Bold),
)

val Typography = Typography(
    // 본문 기본 — 화면들이 fontFamily 를 안 줘도 여기서 Pretendard 를 상속한다.
    bodyLarge = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
)