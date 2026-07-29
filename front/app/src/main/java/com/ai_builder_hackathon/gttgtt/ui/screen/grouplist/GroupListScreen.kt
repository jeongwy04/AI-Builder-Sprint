package com.ai_builder_hackathon.gttgtt.ui.screen.grouplist

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.GroupType
import com.ai_builder_hackathon.gttgtt.ui.component.GroupAvatarStack
import com.ai_builder_hackathon.gttgtt.ui.component.GroupThumbnail
import com.ai_builder_hackathon.gttgtt.ui.component.SearchField
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeBlue
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.component.AlbumLoader
import com.ai_builder_hackathon.gttgtt.ui.component.bounceClick
import com.ai_builder_hackathon.gttgtt.ui.theme.CardShadow
import com.ai_builder_hackathon.gttgtt.ui.theme.DisplayFontFamily
import com.ai_builder_hackathon.gttgtt.ui.theme.CardBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextMuted
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// 시안 .pad / .search / .list 의 좌우 여백은 모두 20
private val ScreenPadding = 20.dp
private const val VISIBLE_AVATAR_COUNT = 3
private val DangerColor = Color(0xFFB64B39)

/**
 * S1 그룹(채팅방) 목록.
 *
 * ViewModel 을 여기서 주입받고, 실제 UI 는 상태만 받는 [GroupListContent] 가 그린다.
 * 이렇게 나눠야 @Preview 가 Hilt 없이 동작한다.
 */
@Composable
fun GroupListScreen(
    onGroupClick: (archiveId: String) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 그룹 삭제·초대 참여 후 이 화면으로 돌아왔을 때 목록이 그대로 남아있지 않도록,
    // 화면이 다시 보일 때마다(RESUME) 새로 불러온다. ViewModel은 백스택에 남아있는 동안
    // 재생성되지 않아 init { loadGroups() } 한 번만으로는 갱신되지 않기 때문.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.retry()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 그룹 사진 변경 — 한 장만 고르면 되니 단일 선택 Photo Picker. 저장소 권한이 필요 없다.
    //
    // ⚠️ Photo Picker 는 별도 액티비티라, 그 사이 화면 회전이나 메모리 회수로 이 화면의
    // ViewModel이 재생성될 수 있다. 그때 uiState.settingsArchiveId 를 다시 읽으면 이미 null로
    // 리셋돼 있어 사진을 골라도 조용히 무시된다 — 그래서 "어느 그룹인지"는 Picker를 띄우는
    // 시점에 rememberSaveable 에 미리 담아둔다. 이건 Bundle에 저장돼 프로세스가 죽어도 살아남는다.
    var pendingCoverImageArchiveId by rememberSaveable { mutableStateOf<String?>(null) }
    val coverImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val archiveId = pendingCoverImageArchiveId
        pendingCoverImageArchiveId = null
        if (uri != null && archiveId != null) {
            viewModel.onCoverImagePicked(archiveId, uri.toString())
        }
    }

    GroupListContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onGroupClick = onGroupClick,
        onProfileClick = onProfileClick,
        onCreateGroupClick = viewModel::onCreateGroupClick,
        onDismissCreateDialog = viewModel::onDismissCreateDialog,
        onCreateNameChange = viewModel::onCreateNameChange,
        onCreateGroupTypeSelect = viewModel::onCreateGroupTypeSelect,
        onConfirmCreateGroup = viewModel::onConfirmCreateGroup,
        onJoinByCodeClick = viewModel::onJoinByCodeClick,
        onDismissJoinDialog = viewModel::onDismissJoinDialog,
        onJoinCodeChange = viewModel::onJoinCodeChange,
        onConfirmJoin = viewModel::onConfirmJoin,
        onSwitchToJoinByCode = viewModel::onSwitchToJoinByCode,
        onGroupSettingsClick = viewModel::onGroupSettingsClick,
        onDismissSettingsSheet = viewModel::onDismissSettingsSheet,
        onChangeCoverImageClick = {
            // 지금 설정 시트가 어느 그룹 것인지 여기서 미리 캡처해둔다 (위 주석 참고).
            pendingCoverImageArchiveId = uiState.settingsArchiveId
            coverImagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onDismissCoverImageError = viewModel::onDismissCoverImageError,
        onRenameClick = viewModel::onRenameClick,
        onDismissRenameDialog = viewModel::onDismissRenameDialog,
        onRenameTextChange = viewModel::onRenameTextChange,
        onConfirmRename = viewModel::onConfirmRename,
        onInviteClick = viewModel::onInviteClick,
        onDismissInviteDialog = viewModel::onDismissInviteDialog,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onConfirmDelete = viewModel::onConfirmDelete,
        modifier = modifier,
    )
}

@Composable
private fun GroupListContent(
    uiState: GroupListUiState,
    onQueryChange: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onDismissCreateDialog: () -> Unit,
    onCreateNameChange: (String) -> Unit,
    onCreateGroupTypeSelect: (GroupType) -> Unit,
    onConfirmCreateGroup: () -> Unit,
    onJoinByCodeClick: () -> Unit = {},
    onDismissJoinDialog: () -> Unit = {},
    onJoinCodeChange: (String) -> Unit = {},
    onConfirmJoin: () -> Unit = {},
    onSwitchToJoinByCode: () -> Unit = {},
    onGroupSettingsClick: (String) -> Unit = {},
    onDismissSettingsSheet: () -> Unit = {},
    onChangeCoverImageClick: () -> Unit = {},
    onDismissCoverImageError: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDismissRenameDialog: () -> Unit = {},
    onRenameTextChange: (String) -> Unit = {},
    onConfirmRename: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onDismissInviteDialog: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onDismissDeleteConfirm: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
        ) {
            HomeHeader(
                onProfileClick = onProfileClick,
                modifier = Modifier.padding(
                    start = ScreenPadding,
                    end = ScreenPadding,
                    top = 6.dp,
                    bottom = 2.dp,
                ),
            )

            SearchField(
                query = uiState.query,
                onQueryChange = onQueryChange,
                placeholder = "채팅방 검색",
                modifier = Modifier.padding(
                    start = ScreenPadding,
                    end = ScreenPadding,
                    top = 14.dp,
                ),
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.updatingCoverImageArchiveId != null) {
                CoverImageStatusBanner(
                    text = "그룹 사진을 올리는 중이에요…",
                    isError = false,
                    onDismiss = null,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 4.dp),
                )
            } else if (uiState.coverImageError != null) {
                CoverImageStatusBanner(
                    text = uiState.coverImageError,
                    isError = true,
                    onDismiss = onDismissCoverImageError,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 4.dp),
                )
            }

            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> MessageState(uiState.errorMessage)
                uiState.isEmpty -> MessageState(
                    if (uiState.query.isBlank()) "아직 참여 중인 채팅방이 없어요." else "검색 결과가 없어요."
                )

                else -> GroupList(
                    groups = uiState.groups,
                    onGroupClick = onGroupClick,
                    onGroupSettingsClick = onGroupSettingsClick,
                )
            }
        }

        CreateGroupFab(
            onClick = onCreateGroupClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }

    if (uiState.isCreateDialogOpen) {
        CreateGroupDialog(
            name = uiState.createName,
            selectedType = uiState.createGroupType,
            isCreating = uiState.isCreating,
            errorMessage = uiState.createError,
            onNameChange = onCreateNameChange,
            onTypeSelect = onCreateGroupTypeSelect,
            onConfirm = onConfirmCreateGroup,
            onDismiss = onDismissCreateDialog,
            onJoinByCodeClick = onSwitchToJoinByCode,
        )
    }

    if (uiState.isJoinDialogOpen) {
        JoinByCodeDialog(
            code = uiState.joinCode,
            isJoining = uiState.isJoining,
            errorMessage = uiState.joinError,
            onCodeChange = onJoinCodeChange,
            onConfirm = onConfirmJoin,
            onDismiss = onDismissJoinDialog,
        )
    }

    if (uiState.isSettingsSheetOpen) {
        GroupSettingsSheet(
            onRenameClick = onRenameClick,
            onInviteClick = onInviteClick,
            onChangeCoverImageClick = onChangeCoverImageClick,
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

    if (uiState.isInviteDialogOpen) {
        InviteFriendDialog(
            isLoading = uiState.isInviteLoading,
            token = uiState.inviteToken,
            errorMessage = uiState.inviteError,
            onDismiss = onDismissInviteDialog,
        )
    }

    if (uiState.isDeleteConfirmOpen) {
        DeleteGroupConfirmDialog(
            groupName = uiState.deletingArchiveName,
            isDeleting = uiState.isDeleting,
            errorMessage = uiState.deleteError,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteConfirm,
        )
    }
}

/** 점 세개 버튼을 누르면 뜨는 그룹 설정 메뉴. 이름 변경 · 친구 초대 · 삭제 세 가지. */
@Composable
private fun GroupSettingsSheet(
    onRenameClick: () -> Unit,
    onInviteClick: () -> Unit,
    onChangeCoverImageClick: () -> Unit,
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
            SettingsMenuRow(label = "그룹 사진 변경", onClick = onChangeCoverImageClick)
            SettingsMenuRow(label = "그룹에 친구 추가", onClick = onInviteClick)
            SettingsMenuRow(label = "그룹 삭제", labelColor = DangerColor, onClick = onDeleteClick)
        }
    }
}

/**
 * 그룹 사진 변경 진행/실패 상태를 알려주는 작은 배너.
 * 이전엔 상태만 들고 있고 화면에 아무것도 그리지 않아서, 실패해도 "아무 반응 없음"으로
 * 보이는 문제가 있었다 — 업로드 중/실패를 여기서 눈에 보이게 알린다.
 */
@Composable
private fun CoverImageStatusBanner(
    text: String,
    isError: Boolean,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isError) DangerColor.copy(alpha = 0.1f) else ChipBackgroundBlue)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (!isError) {
            CircularProgressIndicator(
                color = AbodeBlue,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            color = if (isError) DangerColor else TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = "닫기",
                tint = TextSecondary,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onDismiss),
            )
        }
    }
}

/** 배너 배경으로 쓰는 옅은 파란색. */
private val ChipBackgroundBlue = Color(0xFFE4EEFE)

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
                text = "아래 코드를 친구에게 보내주세요. \"코드로 그룹 참여하기\"에서 입력하면 바로 들어와요.",
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
            )

            Spacer(Modifier.height(18.dp))

            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBackground)
                        .clickable(enabled = !isDeleting, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "취소", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DangerColor)
                        .clickable(enabled = !isDeleting, onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = SurfaceWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(text = "삭제", color = SurfaceWhite, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** 그룹 카드 우측 상단 점 세개 → 그룹 이름 변경 다이얼로그. CreateGroupDialog 와 같은 카드 스타일. */
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
                if (name.isEmpty()) {
                    Text(
                        text = "그룹 이름",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBackground)
                        .clickable(enabled = !isRenaming, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "취소", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandGreen)
                        .clickable(enabled = !isRenaming, onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isRenaming) {
                        CircularProgressIndicator(
                            color = SurfaceWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(text = "저장", color = SurfaceWhite, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** 그룹 설정에서 발급받은 초대 코드를 입력해 그룹에 참여하는 다이얼로그. */
@Composable
private fun JoinByCodeDialog(
    code: String,
    isJoining: Boolean,
    errorMessage: String?,
    onCodeChange: (String) -> Unit,
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
                text = "코드로 그룹 참여하기",
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
                if (code.isEmpty()) {
                    Text(
                        text = "초대 코드",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                BasicTextField(
                    value = code,
                    onValueChange = onCodeChange,
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBackground)
                        .clickable(enabled = !isJoining, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "취소", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandGreen)
                        .clickable(enabled = !isJoining, onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            color = SurfaceWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(text = "참여하기", color = SurfaceWhite, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** 우측 하단에 뜨는 원형 + 버튼. 그룹 만들기 다이얼로그를 연다. */
@Composable
private fun CreateGroupFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(BrandGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = "그룹 만들기",
            tint = SurfaceWhite,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 그룹 이름 + 유형을 받아 새 그룹을 만드는 다이얼로그. */
@Composable
private fun CreateGroupDialog(
    name: String,
    selectedType: GroupType,
    isCreating: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onTypeSelect: (GroupType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onJoinByCodeClick: () -> Unit = {},
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
                text = "새 그룹 만들기",
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
                if (name.isEmpty()) {
                    Text(
                        text = "그룹 이름",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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

            Spacer(Modifier.height(16.dp))

            Text(
                text = "그룹 유형",
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupType.entries.forEach { type ->
                    GroupTypeChip(
                        type = type,
                        selected = type == selectedType,
                        onClick = { onTypeSelect(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBackground)
                        .clickable(enabled = !isCreating, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "취소",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandGreen)
                        .clickable(enabled = !isCreating, onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            color = SurfaceWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            text = "만들기",
                            color = SurfaceWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isCreating, onClick = onJoinByCodeClick),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "코드로 그룹 참여하기",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun GroupTypeChip(
    type: GroupType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) BrandGreen else CardBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.displayName,
            color = if (selected) SurfaceWhite else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 로고(왼쪽) + MY 버튼(오른쪽 끝).
 * 시안에 있던 알림 벨은 제거했다.
 */
@Composable
private fun HomeHeader(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 시안 .logo b : 25px / 900 / italic / letter-spacing -.04em
        // Pretendard 를 res/font/ 에 넣으면 fontFamily 만 지정하면 된다.
        Text(
            text = "그때그때",
            style = TextStyle(
                color = BrandGreen,
                fontFamily = DisplayFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).em,
            ),
        )
        MyButton(onClick = onProfileClick)
    }
}

/** 시안 기준 46x40 의 둥근 사각형. 완전한 알약 형태가 아니다. */
@Composable
private fun MyButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "MY",
            style = TextStyle(
                color = SurfaceWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.02.em,
            ),
        )
    }
}

@Composable
private fun GroupList(
    groups: List<ArchiveSummary>,
    onGroupClick: (String) -> Unit,
    onGroupSettingsClick: (String) -> Unit = {},
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            bottom = 24.dp,
        ),
        // 시안 .list gap: 11
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        items(groups, key = { it.id }) { group ->
            GroupCard(
                group = group,
                onClick = { onGroupClick(group.id) },
                onSettingsClick = { onGroupSettingsClick(group.id) },
            )
        }
    }
}

@Composable
private fun GroupCard(
    group: ArchiveSummary,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Abode 카드: 흰색 + 소프트 섀도우 + 큰 라운드
            .shadow(elevation = 7.dp, shape = RoundedCornerShape(22.dp), spotColor = CardShadow, ambientColor = CardShadow)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceWhite)
            .bounceClick(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            GroupThumbnail(theme = group.theme, imageUrl = group.coverImageUrl)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.02).em,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // 우측 상단 점 세개 버튼과 겹치지 않도록 자리를 비운다.
                    modifier = Modifier.padding(end = 22.dp),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = group.lastMessagePreview,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))

                val shownMembers = group.memberIds.take(VISIBLE_AVATAR_COUNT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    GroupAvatarStack(
                        memberIds = shownMembers,
                        // "+N" 의 N 은 전체 인원에서 실제로 그린 아바타 수를 뺀 값이다.
                        hiddenCount = group.hiddenMemberCount(shownMembers.size),
                    )
                    Text(
                        text = formatTime(group.lastActivityAtMillis),
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // 점 세개 = 그룹 설정(이름 변경 · 친구 초대 · 삭제). 카드 전체 탭(bounceClick) 위에
        // 얹혀도 nested clickable 이 안쪽 우선이라 이것만 반응한다.
        Icon(
            painter = painterResource(R.drawable.ic_dots),
            contentDescription = "그룹 설정",
            tint = TextMuted,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onSettingsClick)
                .padding(4.dp),
        )

        // 안 읽은 메시지 수. 점 세개(우상단)와 시간(우하단) 사이, 우측 가장자리에 세로 중앙 정렬.
        if (group.unreadCount > 0) {
            UnreadCountBadge(
                count = group.unreadCount,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/** 그룹 카드 우측, 점 세개와 시간 사이에 뜨는 안 읽음 개수 배지. 타원이 아닌 정원(正圓) 고정. 99개 넘으면 "99+". */
@Composable
private fun UnreadCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(AbodeBlue),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = SurfaceWhite,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 9.5.sp,
            style = LocalTextStyle.current.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AlbumLoader(modifier = Modifier.padding(top = 48.dp))
    }
}

@Composable
private fun MessageState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 40.dp),
        )
    }
}

// "오후 8:45" 형식. 한국어 로케일을 고정해 기기 설정과 무관하게 시안대로 보이게 한다.
private val TimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(TimeFormatter)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GroupListContentPreview() {
    GttgttTheme {
        GroupListContent(
            uiState = GroupListUiState(
                isLoading = false,
                groups = listOf(
                    ArchiveSummary(
                        id = "1",
                        name = "강릉 여행",
                        lastMessagePreview = "민지: 바다 너무 예뻤어",
                        lastActivityAtMillis = 0L,
                        theme = GradientTheme.BEACH,
                        memberIds = listOf("u-minji", "u-seoyeon", "u-jaehun"),
                        totalMemberCount = 6,
                        unreadCount = 3,
                    ),
                    ArchiveSummary(
                        id = "2",
                        name = "우리 대학 동기들",
                        lastMessagePreview = "현우: 다음에 또 보자 ㅋㅋ",
                        lastActivityAtMillis = 0L,
                        theme = GradientTheme.FOREST,
                        memberIds = listOf("u-hyunwoo", "u-doyun"),
                        totalMemberCount = 6,
                        unreadCount = 128,
                    ),
                    ArchiveSummary(
                        id = "3",
                        name = "가족",
                        lastMessagePreview = "엄마: 주말에 만나자~",
                        lastActivityAtMillis = 0L,
                        theme = GradientTheme.FAMILY,
                        memberIds = listOf("u-mom", "u-dad"),
                        totalMemberCount = 4,
                    ),
                    ArchiveSummary(
                        id = "4",
                        name = "개발팀",
                        lastMessagePreview = "지훈: PR 리뷰 부탁드려요!",
                        lastActivityAtMillis = 0L,
                        theme = GradientTheme.LAPTOP,
                        memberIds = listOf("u-jihun", "u-sora", "u-taeyang"),
                        totalMemberCount = 6,
                    ),
                ),
            ),
            onQueryChange = {},
            onGroupClick = {},
            onProfileClick = {},
            onCreateGroupClick = {},
            onDismissCreateDialog = {},
            onCreateNameChange = {},
            onCreateGroupTypeSelect = {},
            onConfirmCreateGroup = {},
        )
    }
}
