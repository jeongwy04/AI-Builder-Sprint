package com.ai_builder_hackathon.gttgtt.ui.screen.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.component.bounceClick
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeBlue
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeBlueTint
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeCoral
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeYellow
import com.ai_builder_hackathon.gttgtt.ui.theme.DisplayFontFamily
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OnboardingEntryPoint {
    fun supabaseClient(): SupabaseClient
}

private data class OnboardingPage(
    val heading: String,
    val sub: String,
)

private val PAGES = listOf(
    OnboardingPage("폰 속에서\n함께 추억을", "사진이 아니라, 그날의 이야기를\n함께 남기는 공간"),
    OnboardingPage("말 한마디로\n추억을 찾아요", "\"바다\", \"첫눈\"처럼 편하게 물어보면\nAI가 그날을 찾아줘요"),
    OnboardingPage("함께라서\n더 특별하게", "그룹 멤버와 메모·댓글·좋아요로\n기억을 나눠요"),
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onAlreadyAuthed: () -> Unit,
) {
    // 이미 로그인돼 있으면 온보딩을 건너뛴다 (세션은 supabase-kt 가 자동 복원).
    val appContext = LocalContext.current.applicationContext
    val supabase = remember(appContext) {
        EntryPointAccessors
            .fromApplication(appContext, OnboardingEntryPoint::class.java)
            .supabaseClient()
    }
    var navigated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        supabase.auth.sessionStatus.collect { status ->
            if (status is SessionStatus.Authenticated && !navigated) {
                navigated = true
                onAlreadyAuthed()
            }
        }
    }

    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == PAGES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .padding(horizontal = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dots(count = PAGES.size, current = pager.currentPage)
            Spacer(Modifier.weight(1f))
            Text(
                text = "로그인",
                color = AbodeBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(AbodeBlueTint)
                    .clickable(onClick = onFinished)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            )
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { index ->
            PageContent(page = PAGES[index], pageIndex = index)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(AbodeBlue)
                .bounceClick {
                    if (isLast) onFinished()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isLast) "시작하기" else "다음",
                color = SurfaceWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Text(
            text = "가입 시 이용약관 및 개인정보처리방침에 동의하게 됩니다",
            color = TextSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun PageContent(page: OnboardingPage, pageIndex: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (pageIndex) {
                0 -> BlobCluster()
                1 -> IconBubble(AbodeYellow, R.drawable.ic_solar_spark)
                else -> IconBubble(AbodeCoral, R.drawable.ic_heart)
            }
        }
        Text(
            text = page.heading,
            color = TextPrimary,
            fontFamily = DisplayFontFamily,
            fontSize = 32.sp,
            lineHeight = 38.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.sub,
            color = TextSecondary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(20.dp))
    }
}

/** 페이지 1 — 통통한 말풍선 블롭 3개 (Abode 캐릭터 느낌). */
@Composable
private fun BlobCluster() {
    Box(modifier = Modifier.size(width = 230.dp, height = 190.dp)) {
        Blob(AbodeBlue, 100.dp, 88.dp, Modifier.align(Alignment.TopStart).offset(x = 16.dp, y = 24.dp))
        Blob(AbodeYellow, 88.dp, 80.dp, Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 8.dp))
        Blob(AbodeCoral, 82.dp, 72.dp, Modifier.align(Alignment.BottomCenter).offset(x = 10.dp))
    }
}

@Composable
private fun Blob(color: Color, w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    Row(
        modifier = modifier
            .size(width = w, height = h)
            .clip(RoundedCornerShape(topStartPercent = 52, topEndPercent = 48, bottomEndPercent = 55, bottomStartPercent = 45))
            .background(color),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eye(); Spacer(Modifier.width(8.dp)); Eye()
    }
}

@Composable
private fun Eye() {
    Box(
        modifier = Modifier
            .size(width = 9.dp, height = 13.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceWhite),
    )
}

@Composable
private fun IconBubble(color: Color, iconRes: Int) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = SurfaceWhite,
            modifier = Modifier.size(52.dp),
        )
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            val w by animateDpAsState(if (active) 20.dp else 6.dp, label = "dotWidth")
            Box(
                modifier = Modifier
                    .size(width = w, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) AbodeBlue else Color(0xFFD7DBE3)),
            )
        }
    }
}
