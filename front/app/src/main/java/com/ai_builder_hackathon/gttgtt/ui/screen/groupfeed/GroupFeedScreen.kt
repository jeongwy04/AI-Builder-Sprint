package com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.asPhoto
import com.ai_builder_hackathon.gttgtt.domain.model.Post
import com.ai_builder_hackathon.gttgtt.ui.component.AppTopBar
import com.ai_builder_hackathon.gttgtt.ui.component.GroupBottomNavBar
import com.ai_builder_hackathon.gttgtt.ui.component.MemberAvatar
import com.ai_builder_hackathon.gttgtt.ui.component.PhotoImage
import com.ai_builder_hackathon.gttgtt.ui.component.TopBarButton
import com.ai_builder_hackathon.gttgtt.ui.screen.chat.AiChatPanel
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.CardBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.ChipBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.ChipText
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.LikeChipBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.LikeChipText
import com.ai_builder_hackathon.gttgtt.ui.theme.MoreIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import com.ai_builder_hackathon.gttgtt.ui.theme.TopBarIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 삭제 확인 등 위험한 동작에 쓰는 경고색. GroupListScreen 의 에러 텍스트와 같은 톤. */
private val DangerColor = Color(0xFFB64B39)

private val ScreenPadding = 20.dp

/** AI 패널이 차지하는 화면 비율. 뒤의 피드가 보여야 맥락이 산다. */
private const val AI_PANEL_HEIGHT_FRACTION = 0.58f

/** 패널이 열렸을 때 뒤 피드를 덮는 반투명 막 */
private val ScrimColor = Color(0x66000000)

@Composable
fun GroupFeedScreen(
    onBackClick: () -> Unit,
    onChatClick: () -> Unit,
    onMemoryClick: (String) -> Unit,
    onCreateMemoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupFeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // AI 대화는 별도 화면이 아니라 이 피드 위에 뜨는 패널이다.
    // 뒤에 피드가 보여야 "이 그룹 안에서 찾는다"는 맥락이 유지된다.
    var isAiSheetOpen by rememberSaveable { mutableStateOf(false) }

    // 패널이 열려 있으면 뒤로가기가 화면을 나가는 대신 패널을 닫는다.
    BackHandler(enabled = isAiSheetOpen) { isAiSheetOpen = false }

    // 그룹 삭제가 끝나면 이 화면에 더 보여줄 게 없다 — 목록으로 나간다.
    LaunchedEffect(uiState.isGroupDeleted) {
        if (uiState.isGroupDeleted) onBackClick()
    }

    GroupFeedContent(
        uiState = uiState,
        isAiSheetOpen = isAiSheetOpen,
        onBackClick = onBackClick,
        onChatClick = onChatClick,
        onAiSearchClick = { isAiSheetOpen = !isAiSheetOpen },
        onDismissAiSheet = { isAiSheetOpen = false },
        onCreateMemoryClick = onCreateMemoryClick,
        onLikeClick = viewModel::onLikeClick,
        onPostClick = onMemoryClick,
        onMemoryClickFromAi = { memoryId ->
            isAiSheetOpen = false
            onMemoryClick(memoryId)
        },
        onSettingsClick = viewModel::onSettingsClick,
        onDismissSettingsSheet = viewModel::onDismissSettingsSheet,
        onRenameClick = viewModel::onRenameClick,
        onDeleteClick = viewModel::onDeleteClick,
        onInviteClick = viewModel::onInviteClick,
        onRenameTextChange = viewModel::onRenameTextChange,
        onDismissRenameDialog = viewModel::onDismissRenameDialog,
        onConfirmRename = viewModel::onConfirmRename,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onConfirmDelete = viewModel::onConfirmDelete,
        onDismissInviteDialog = viewModel::onDismissInviteDialog,
        modifier = modifier,
    )
}

@Composable
private fun GroupFeedContent(
    uiState: GroupFeedUiState,
    isAiSheetOpen: Boolean,
    onBackClick: () -> Unit,
    onChatClick: () -> Unit,
    onAiSearchClick: () -> Unit,
    onDismissAiSheet: () -> Unit,
    onCreateMemoryClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onMemoryClickFromAi: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onDismissSettingsSheet: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onRenameTextChange: (String) -> Unit = {},
    onDismissRenameDialog: () -> Unit = {},
    onConfirmRename: () -> Unit = {},
    onDismissDeleteConfirm: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissInviteDialog: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 채팅·AI·기억 남기기가 전부 하단 바로 내려가서 상단엔 뒤로가기/제목/설정만 남는다.
            AppTopBar(
                title = uiState.groupName,
                subtitle = "멤버 ${uiState.memberCount}명",
                onBackClick = onBackClick,
                action = {
                    TopBarButton(
                        iconRes = R.drawable.ic_settings,
                        contentDescription = "그룹 설정",
                        background = SurfaceWhite,
                        tint = TopBarIcon,
                        onClick = onSettingsClick,
                    )
                },
            )

            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> MessageState(uiState.errorMessage)
                uiState.isEmpty -> MessageState("아직 올라온 추억이 없어요.")
                else -> PostList(
                    posts = uiState.posts,
                    onLikeClick = onLikeClick,
                    onPostClick = onPostClick,
                )
            }
        }

        // 패널 뒤의 피드를 살짝 어둡게. 탭하면 닫힌다.
        if (isAiSheetOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrimColor)
                    .clickable(onClick = onDismissAiSheet),
            )
        }

        // 바와 패널을 한 Column 에 쌓아서, 패널이 펼쳐지면 바가 그 위에 얹혀 함께 올라간다.
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GroupBottomNavBar(
                // AI 패널이 열려 있는 동안 AI 탭이 채워진 상태로 보인다.
                isAiSelected = isAiSheetOpen,
                onAiClick = onAiSearchClick,
                onChatClick = onChatClick,
                onCreateMemoryClick = onCreateMemoryClick,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            AnimatedVisibility(
                visible = isAiSheetOpen,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                AiChatPanel(
                    onMemoryClick = onMemoryClickFromAi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(AI_PANEL_HEIGHT_FRACTION),
                )
            }
        }
    }

    if (uiState.isSettingsSheetOpen) {
        GroupSettingsSheet(
            onRenameClick = onRenameClick,
            onInviteClick = onInviteClick,
            onDeleteClick = onDeleteClick,
            onDismiss = onDismissSettingsSheet,
        )
    }

    if (uiState.isRenameDialogOpen) {
        RenameGroupDialog(
            name = uiState.renameText,
            isRenaming = uiState.isRenaming,
            errorMessage = uiState.renameError,
            onNameChange = onRenameTextChange,
            onConfirm = onConfirmRename,
            onDismiss = onDismissRenameDialog,
        )
    }

    if (uiState.isDeleteConfirmOpen) {
        DeleteGroupConfirmDialog(
            groupName = uiState.groupName,
            isDeleting = uiState.isDeleting,
            errorMessage = uiState.deleteError,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteConfirm,
        )
    }

    if (uiState.isInviteDialogOpen) {
        InviteFriendDialog(
            isLoading = uiState.isInviteLoading,
            token = uiState.inviteToken,
            errorMessage = uiState.inviteError,
            onDismiss = onDismissInviteDialog,
        )
    }
}

/** 톱니 버튼을 누르면 뜨는 그룹 설정 메뉴. 이름 변경 · 친구 초대 · 삭제 세 가지. */
@Composable
private fun GroupSettingsSheet(
    onRenameClick: () -> Unit,
    onInviteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = "그룹 설정",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            SettingsMenuRow(label = "그룹 이름 변경", onClick = onRenameClick)
            SettingsMenuRow(label = "그룹에 친구 추가", onClick = onInviteClick)
            SettingsMenuRow(label = "그룹 삭제", labelColor = DangerColor, onClick = onDeleteClick)
        }
    }
}

@Composable
private fun SettingsMenuRow(
    label: String,
    onClick: () -> Unit,
    labelColor: Color = TextPrimary,
) {
    Text(
        text = label,
        color = labelColor,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

/** 그룹 이름 변경 다이얼로그. CreateGroupDialog(GroupListScreen) 와 같은 카드 스타일을 쓴다. */
@Composable
private fun RenameGroupDialog(
    name: String,
    isRenaming: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(24.dp),
        ) {
            Text(
                text = "그룹 이름 변경",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
            )

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ScreenBackground)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = DangerColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(20.dp))

            DialogActionRow(
                confirmLabel = "변경",
                isLoading = isRenaming,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

/** 그룹 삭제 확인 다이얼로그. 되돌릴 수 없다는 걸 분명히 알린다. */
@Composable
private fun DeleteGroupConfirmDialog(
    groupName: String,
    isDeleting: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(24.dp),
        ) {
            Text(
                text = "그룹을 삭제할까요?",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "\"$groupName\"의 모든 추억과 대화가 함께 삭제되고, 되돌릴 수 없어요.",
                color = TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = DangerColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(20.dp))

            DialogActionRow(
                confirmLabel = "삭제",
                confirmColor = DangerColor,
                isLoading = isDeleting,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

/** 친구 초대 코드 다이얼로그. 발급 중엔 스피너, 끝나면 코드 + 복사/공유. */
@Composable
private fun InviteFriendDialog(
    isLoading: Boolean,
    token: String?,
    errorMessage: String?,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(24.dp),
        ) {
            Text(
                text = "친구 초대",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "아래 코드를 친구에게 보내주세요. 그룹 목록의 \"코드로 참여하기\"에서 입력하면 바로 들어와요.",
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
            )

            Spacer(Modifier.height(18.dp))

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandGreen, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }

                errorMessage != null -> Text(
                    text = errorMessage,
                    color = DangerColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                token != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ScreenBackground)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = token,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.05.em,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBackground)
                                .clickable { clipboard.setText(AnnotatedString(token)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "코드 복사", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(BrandGreen)
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "그때그때 그룹에 초대할게요! 코드: $token",
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "초대 코드 공유"))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "공유하기", color = SurfaceWhite, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "닫기", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 다이얼로그 하단의 취소/확인 버튼 한 쌍. 그룹 이름 변경·삭제 확인이 같은 모양을 쓴다. */
@Composable
private fun DialogActionRow(
    confirmLabel: String,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColor: Color = BrandGreen,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .clickable(enabled = !isLoading, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "취소", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(confirmColor)
                .clickable(enabled = !isLoading, onClick = onConfirm),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = SurfaceWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(text = confirmLabel, color = SurfaceWhite, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PostList(
    posts: List<Post>,
    onLikeClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
) {
    LazyColumn(
        // FAB 에 마지막 카드가 가리지 않도록 아래를 넉넉히 비운다.
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onLikeClick = { onLikeClick(post.id) },
                onClick = { onPostClick(post.id) },
            )
        }
    }
}

/** 시안 .post — 흰 카드, radius 24, 그림자 */
@Composable
private fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)
    ) {
        PostHeader(post)
        Spacer(Modifier.height(12.dp))
        PostPhotos(post)
        Text(
            text = post.caption,
            style = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.6.sp,
                letterSpacing = (-0.01).em,
            ),
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 12.dp),
        )
        PostFooter(post = post, onLikeClick = onLikeClick)
    }
}

@Composable
private fun PostHeader(post: Post) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MemberAvatar(memberId = post.authorId, size = 34.dp, showRing = false)
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
            Text(
                text = post.authorName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = " · ${formatDate(post.memoryDateMillis)}",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_dots),
            contentDescription = "더보기",
            tint = MoreIcon,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 사진 1장이면 큰 히어로(h170, r18), 여러 장이면 3열 그리드(h92, r14). */
@Composable
private fun PostPhotos(post: Post) {
    if (post.hasSinglePhoto) {
        PhotoImage(
            photo = post.photos.first(),
            corner = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            post.photos.take(3).forEach { photo ->
                PhotoImage(
                    photo = photo,
                    corner = 14.dp,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                )
            }
            // 3장이 안 되면 남은 칸을 비워 그리드 폭을 유지한다.
            repeat(3 - post.photos.size.coerceAtMost(3)) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PostFooter(post: Post, onLikeClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Chip(
            iconRes = if (post.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            label = post.likeCount.toString(),
            background = LikeChipBackground,
            contentColor = LikeChipText,
            onClick = onLikeClick,
        )
        Chip(
            iconRes = R.drawable.ic_message_circle,
            label = post.commentCount.toString(),
            background = ChipBackground,
            contentColor = ChipText,
            onClick = { /* TODO: 댓글 화면 시안 나오면 연결 */ },
        )
    }
}

@Composable
private fun Chip(
    iconRes: Int,
    label: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        CircularProgressIndicator(
            color = BrandGreen,
            modifier = Modifier.padding(top = 40.dp),
        )
    }
}

@Composable
private fun MessageState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 40.dp),
        )
    }
}

// 시안 표기: "2025.12.22"
private val DateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateFormatter)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GroupFeedContentPreview() {
    GttgttTheme {
        GroupFeedContent(
            uiState = GroupFeedUiState(
                isLoading = false,
                groupName = "강릉 여행",
                memberCount = 5,
                posts = listOf(
                    Post(
                        id = "1",
                        archiveId = "a",
                        authorId = "u-minji",
                        authorName = "민지",
                        memoryDateMillis = 1_766_361_600_000L,
                        photos = listOf(GradientTheme.BEACH.asPhoto()),
                        caption = "시험 끝나고 치킨 먹다 다 같이 울었던 날 🥹",
                        likeCount = 12,
                        commentCount = 5,
                    ),
                    Post(
                        id = "2",
                        archiveId = "a",
                        authorId = "u-hyunwoo",
                        authorName = "현우",
                        memoryDateMillis = 1_766_275_200_000L,
                        photos = listOf(
                            GradientTheme.SEA.asPhoto(),
                            GradientTheme.BEACH.asPhoto(),
                            GradientTheme.FOREST.asPhoto(),
                        ),
                        caption = "바다 진짜 예뻤다! 날씨도 완벽 ☀️",
                        likeCount = 8,
                        commentCount = 2,
                        likedByMe = true,
                    ),
                ),
            ),
            isAiSheetOpen = false,
            onBackClick = {},
            onChatClick = {},
            onAiSearchClick = {},
            onDismissAiSheet = {},
            onCreateMemoryClick = {},
            onLikeClick = {},
            onPostClick = {},
            onMemoryClickFromAi = {},
        )
    }
}
