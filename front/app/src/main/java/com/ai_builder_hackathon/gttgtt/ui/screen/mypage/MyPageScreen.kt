package com.ai_builder_hackathon.gttgtt.ui.screen.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile
import com.ai_builder_hackathon.gttgtt.ui.component.AlbumLoader
import com.ai_builder_hackathon.gttgtt.ui.component.MemberAvatar
import com.ai_builder_hackathon.gttgtt.ui.theme.DisplayFontFamily
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
import com.ai_builder_hackathon.gttgtt.ui.theme.ChevronIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.MenuDivider
import com.ai_builder_hackathon.gttgtt.ui.theme.MenuItemIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.MenuItemText
import com.ai_builder_hackathon.gttgtt.ui.theme.MyPageHeaderGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.MyPageSettingsIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.MyPageSubText
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.StatMediaBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.StatMediaIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.StatMemoryBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary

private val SidePadding = 20.dp

@Composable
fun MyPageScreen(
    onBackClick: () -> Unit,
    onSignedOut: () -> Unit,
    onMyMemoriesClick: () -> Unit,
    onLikedClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 로그아웃이 끝나면 화면 밖으로 내보내는 건 네비게이션의 몫이라 여기서 알린다.
    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) onSignedOut()
    }

    MyPageContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onSignOutClick = viewModel::onSignOutClick,
        onStatusSave = viewModel::onStatusSave,
        onMyMemoriesClick = onMyMemoriesClick,
        onLikedClick = onLikedClick,
        modifier = modifier,
    )
}

@Composable
private fun MyPageContent(
    uiState: MyPageUiState,
    onBackClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onStatusSave: (String) -> Unit,
    onMyMemoriesClick: () -> Unit,
    onLikedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHelp by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var editingStatus by remember { mutableStateOf(false) }
    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
    if (showWithdraw) {
        WithdrawDialog(onDismiss = { showWithdraw = false })
    }
    if (editingStatus && uiState.profile != null) {
        StatusEditDialog(
            current = uiState.profile.statusMessage,
            onSave = {
                onStatusSave(it)
                editingStatus = false
            },
            onDismiss = { editingStatus = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                    contentAlignment = Alignment.Center,
                ) { AlbumLoader() }
            }

            uiState.profile == null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.errorMessage ?: "프로필을 불러오지 못했습니다.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            else -> {
                ProfileHeader(
                    profile = uiState.profile,
                    onBackClick = onBackClick,
                    onStatusClick = { editingStatus = true },
                )

                Spacer(Modifier.height(12.dp))
                StatCard(
                    iconRes = R.drawable.ic_notebook,
                    iconBackground = StatMemoryBackground,
                    iconTint = BrandGreenDark,
                    label = "내가 남긴 추억",
                    value = uiState.profile.memoryCount,
                    onClick = onMyMemoriesClick,
                )
                MenuCard(
                    onLikedClick = onLikedClick,
                    onSignOutClick = onSignOutClick,
                    onHelpClick = { showHelp = true },
                    onWithdrawClick = { showWithdraw = true },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** 시안 .mp-head — 민트→연보라 그라디언트, 아래 모서리만 크게 둥글다. */
@Composable
private fun ProfileHeader(
    profile: UserProfile,
    onBackClick: () -> Unit,
    onStatusClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .background(MyPageHeaderGradient)
            .padding(horizontal = 24.dp, vertical = 26.dp)
    ) {
        // 시안엔 없지만 진입 경로가 홈이라 뒤로가기가 필요하다.
        Icon(
            painter = painterResource(R.drawable.ic_chevron_left),
            contentDescription = "뒤로",
            tint = MyPageSettingsIcon,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(24.dp)
                .clickable(onClick = onBackClick),
        )
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "설정",
            tint = MyPageSettingsIcon,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .clickable { /* TODO: 설정 화면 시안 나오면 연결 */ },
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarWithCamera(profile)

            Spacer(Modifier.height(14.dp))
            Text(
                text = profile.name,
                style = TextStyle(
                    color = TextPrimary,
                    fontFamily = DisplayFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).em,
                ),
            )

            Spacer(Modifier.height(10.dp))
            StreakBadge(days = profile.streakDays)

            Spacer(Modifier.height(10.dp))
            // 탭하면 상태 메시지를 직접 편집한다 (SNS 한 줄 상태처럼).
            Text(
                text = profile.statusMessage,
                color = MyPageSubText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onStatusClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun AvatarWithCamera(profile: UserProfile) {
    Box {
        MemberAvatar(
            memberId = profile.id,
            size = 92.dp,
            showRing = false,
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                .border(width = 4.dp, color = SurfaceWhite, shape = CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(30.dp)
                .clip(CircleShape)
                .background(BrandGreen)
                .border(width = 3.dp, color = SurfaceWhite, shape = CircleShape)
                .clickable { /* TODO: 프로필 사진 변경 */ },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = "프로필 사진 변경",
                tint = SurfaceWhite,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun StreakBadge(days: Int) {
    Row(
        modifier = Modifier
            .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flame),
            contentDescription = null,
            tint = BrandGreenDark,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "그때그때 ${days}일째",
            color = BrandGreenDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** 시안 .statcard */
@Composable
private fun StatCard(
    iconRes: Int,
    iconBackground: Color,
    iconTint: Color,
    label: String,
    value: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = SidePadding)
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.toString(),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        ChevronRight()
    }
}

/** 시안 .menucard — 항목 사이에만 얇은 구분선이 들어간다. */
@Composable
private fun MenuCard(
    onLikedClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onHelpClick: () -> Unit,
    onWithdrawClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = SidePadding, vertical = 18.dp)
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
    ) {
        MenuItem(R.drawable.ic_heart, "좋아요한 추억", onClick = onLikedClick)
        HorizontalDivider(thickness = 1.dp, color = MenuDivider)
        MenuItem(R.drawable.ic_settings, "설정") { /* TODO: 설정 화면 */ }
        HorizontalDivider(thickness = 1.dp, color = MenuDivider)
        MenuItem(R.drawable.ic_help_circle, "도움말", onClick = onHelpClick)
        HorizontalDivider(thickness = 1.dp, color = MenuDivider)
        MenuItem(R.drawable.ic_logout, "로그아웃", onClick = onSignOutClick)
        HorizontalDivider(thickness = 1.dp, color = MenuDivider)
        MenuItem(R.drawable.ic_logout, "회원 탈퇴", onClick = onWithdrawClick)
    }
}

/** 앱 사용법을 알려주는 도움말 다이얼로그. */
@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = BrandGreenDark, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = "그때그때 도움말",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Text(
                text = "• 그룹을 만들고 사진과 함께 그날의 이야기(메모)를 남겨보세요.\n\n" +
                    "• 추억을 찾을 땐 'AI 추억 찾기'에 \"바다\", \"첫눈\"처럼 편하게 말하면 " +
                    "관련된 기억을 찾아줍니다.\n\n" +
                    "• 검색은 사진이 아니라 여러분이 남긴 문장을 기준으로 동작해요.\n\n" +
                    "• 그룹 멤버는 서로의 기억에 메모·댓글·좋아요를 남길 수 있어요.",
                color = TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
            )
        },
        containerColor = SurfaceWhite,
    )
}

/** 상태 메시지를 직접 작성·저장하는 다이얼로그 (SNS 한 줄 상태처럼). */
@Composable
private fun StatusEditDialog(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("저장", color = BrandGreenDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        },
        title = {
            Text("상태 메시지", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        },
        text = {
            OutlinedTextField(
                value = text,
                // 60자 제한 — 한 줄 상태라 짧게.
                onValueChange = { if (it.length <= 60) text = it },
                placeholder = { Text("지금 기분이나 한마디를 남겨보세요") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        containerColor = SurfaceWhite,
    )
}

/** 회원 탈퇴 확인 다이얼로그. 실제 삭제 로직은 아직 붙이지 않은 placeholder 다. */
@Composable
private fun WithdrawDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = BrandGreenDark, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text("회원 탈퇴", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        },
        text = {
            Text(
                text = "회원 탈퇴 시 모든 추억과 메모가 삭제되며 되돌릴 수 없어요.\n\n" +
                    "이 기능은 현재 준비 중이에요.",
                color = TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
            )
        },
        containerColor = SurfaceWhite,
    )
}

@Composable
private fun MenuItem(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MenuItemIcon,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = label,
            color = MenuItemText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        ChevronRight()
    }
}

/** 오른쪽 꺾쇠. 아이콘을 하나 더 만들지 않으려고 chevron-left 를 뒤집어 쓴다. */
@Composable
private fun ChevronRight() {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_left),
        contentDescription = null,
        tint = ChevronIcon,
        modifier = Modifier
            .size(17.dp)
            .rotate(180f),
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MyPageContentPreview() {
    GttgttTheme {
        MyPageContent(
            uiState = MyPageUiState(
                isLoading = false,
                profile = UserProfile(
                    id = "u-me",
                    name = "김그때",
                    streakDays = 120,
                    statusMessage = "추억을 모으는 중 ✨",
                    memoryCount = 128,
                    mediaCount = 342,
                ),
            ),
            onBackClick = {},
            onSignOutClick = {},
            onStatusSave = {},
            onMyMemoriesClick = {},
            onLikedClick = {},
        )
    }
}
