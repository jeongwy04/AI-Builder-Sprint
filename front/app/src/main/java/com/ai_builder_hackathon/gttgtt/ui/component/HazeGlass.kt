package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Haze(진짜 backdrop blur) 사용을 이 파일 하나로 격리한다.
 *
 * 이유: Haze 는 버전마다 심볼 이름이 바뀌었다(hazeChild → hazeEffect, haze → hazeSource 등).
 * 빌드 환경에서 컴파일을 확인할 수 없으므로, API 가 다르면 이 파일만 고치면 되도록
 * 나머지 화면/컴포넌트는 여기 노출한 확장함수(hazeBackdrop / frostedGlass)만 쓴다.
 *
 * 현재 기준(Haze 1.x, hazeEffect/hazeSource):
 *  - 배경(블러의 원본)에 [hazeBackdrop] 를 건다.
 *  - 그 위에 뜨는 유리 요소에 [frostedGlass] 를 건다(같은 [HazeState] 공유).
 */
@Composable
fun rememberAppHazeState(): HazeState = remember { HazeState() }

/** 이 노드의 내용이 위에 뜬 유리에 비쳐 블러의 원본이 된다. */
fun Modifier.hazeBackdrop(state: HazeState): Modifier = this.hazeSource(state)

/**
 * 뒤 배경([hazeBackdrop] 를 건 콘텐츠)을 블러 처리해 유리처럼 보이게 한다.
 * @param tint 유리 표면에 얹는 색(다크/라이트 톤). 반투명이어야 배경이 비친다.
 */
fun Modifier.frostedGlass(
    state: HazeState,
    shape: Shape,
    tint: Color,
    blurRadius: Dp = 24.dp,
    borderColor: Color = Color.White.copy(alpha = 0.20f),
): Modifier = this
    .clip(shape)
    .hazeEffect(
        state = state,
        style = HazeStyle(
            backgroundColor = tint,
            tints = listOf(HazeTint(tint)),
            blurRadius = blurRadius,
        ),
    )
    .border(width = 1.dp, color = borderColor, shape = shape)
