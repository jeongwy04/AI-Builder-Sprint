package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.ai_builder_hackathon.gttgtt.ui.theme.CardBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary

// 시안 .search : radius 16, padding 14/16, gap 10, 14.5px weight 600
private val FieldCorner = 16.dp
private val IconSize = 18.dp
private val FieldFontSize = 14.5.sp

/**
 * 회색 배경의 둥근 검색 입력창.
 *
 * Material3 TextField 는 밑줄·라벨·기본 패딩이 붙어 시안과 맞지 않아 BasicTextField 로 직접 그린다.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldCorner))
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchIcon(tint = TextSecondary)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = TextSecondary,
                    fontSize = FieldFontSize,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = TextPrimary,
                    fontSize = FieldFontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 돋보기 아이콘을 직접 그린다.
 * material-icons 아티팩트를 추가하지 않기 위한 선택 — 아이콘 하나 때문에 의존성을 늘리지 않는다.
 */
@Composable
private fun SearchIcon(tint: Color) {
    Canvas(modifier = Modifier.size(IconSize)) {
        val stroke = size.minDimension * 0.11f
        val radius = size.minDimension * 0.32f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)

        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = stroke),
        )
        // 손잡이: 원의 오른쪽 아래 45도 방향
        val handleStart = Offset(
            x = center.x + radius * 0.72f,
            y = center.y + radius * 0.72f,
        )
        drawLine(
            color = tint,
            start = handleStart,
            end = Offset(size.width * 0.88f, size.height * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchFieldPreview() {
    GttgttTheme {
        SearchField(
            query = "",
            onQueryChange = {},
            placeholder = "채팅방 검색",
            modifier = Modifier.padding(16.dp),
        )
    }
}
