package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.theme.DisplayFontFamily
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import com.ai_builder_hackathon.gttgtt.ui.theme.TopBarIcon

// 시안 .tbar : padding 8/20, gap 12 / .bk, .round : 38x38, radius 13
private val ActionSize = 38.dp
private val ActionCorner = 13.dp

/**
 * 뒤로가기 + 제목/부제 + 우측 액션 1개로 구성된 상단바.
 * 시안의 피드·채팅·상세 화면이 모두 이 형태라 공통 컴포넌트로 뺐다.
 */
@Composable
fun AppTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** 그룹 채팅방처럼 제목을 바 중앙에 크게 놓고 싶을 때 켠다 (헤딩 폰트 + 더 크고 굵게). */
    centerTitle: Boolean = false,
    /** 제목을 눌렀을 때 할 동작 — 예: 제목을 누르면 그룹 설정이 열리는 화면. */
    onTitleClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopBarButton(
            iconRes = R.drawable.ic_chevron_left,
            contentDescription = "뒤로",
            background = SurfaceWhite,
            tint = TopBarIcon,
            onClick = onBackClick,
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = if (centerTitle) DisplayFontFamily else null,
                    fontSize = if (centerTitle) 21.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.02).em,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // 클릭 영역을 글자 자기 너비만큼만 준다 — Column 전체(weight(1f))에 걸면
                // 빈 여백까지 눌리는 버튼처럼 커져 보인다.
                modifier = if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (action != null) {
            action()
        } else if (centerTitle) {
            // 우측 액션이 없으면 왼쪽 뒤로가기 버튼만큼 빈 자리를 남겨 제목이 바 전체
            // 기준으로 정말 가운데에 오게 한다 (weight(1f) 만으로는 왼쪽 버튼 쪽으로 치우친다).
            Box(modifier = Modifier.size(ActionSize))
        }
    }
}

/** 상단바의 정사각 둥근 버튼. 좌측 뒤로가기와 우측 액션이 같은 규격을 쓴다. */
@Composable
fun TopBarButton(
    iconRes: Int,
    contentDescription: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ActionSize)
            .clip(RoundedCornerShape(ActionCorner))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppTopBarPreview() {
    GttgttTheme {
        AppTopBar(
            title = "강릉 여행",
            subtitle = "멤버 5명",
            onBackClick = {},
        )
    }
}
