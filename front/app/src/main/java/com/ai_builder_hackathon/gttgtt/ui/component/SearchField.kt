package com.ai_builder_hackathon.gttgtt.ui.component

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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.theme.CardShadow
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState

// 그룹 피드 게시물 박스와 동일한 코너(RoundedCornerShape 24). padding 15/18, gap 10.
private val FieldCorner = 24.dp
private val IconSize = 18.dp
private val FieldFontSize = 14.5.sp
// 라이트 프로스트 글래스 — 어두운 입력 글자가 읽히도록 흰 반투명 톤을 쓴다.
private val LightGlassTint = Color.White.copy(alpha = 0.42f)
private val LightGlassBorder = Color.White.copy(alpha = 0.55f)

/**
 * 둥근 검색 입력창. [hazeState] 를 주면 뒤 배경이 비치는 프로스트 글래스로, 없으면 흰 카드로 그린다.
 *
 * Material3 TextField 는 밑줄·라벨·기본 패딩이 붙어 시안과 맞지 않아 BasicTextField 로 직접 그린다.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val fieldShape = RoundedCornerShape(FieldCorner)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = fieldShape, spotColor = CardShadow, ambientColor = CardShadow)
            .then(
                if (hazeState != null) {
                    Modifier.frostedGlass(hazeState, fieldShape, tint = LightGlassTint, blurRadius = 20.dp, borderColor = LightGlassBorder)
                } else {
                    Modifier.clip(fieldShape).background(SurfaceWhite)
                }
            )
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(IconSize),
        )
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
