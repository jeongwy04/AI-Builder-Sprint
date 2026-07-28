package com.ai_builder_hackathon.gttgtt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 시안이 라이트 전용이라 다크 스킴을 만들지 않는다.
// 다크 대응은 시안이 나온 뒤에 추가한다 — 지금 임의로 만들면 시안과 어긋난다.
private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = SurfaceWhite,
    secondary = BrandGreenDark,
    onSecondary = SurfaceWhite,
    background = ScreenBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun GttgttTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
