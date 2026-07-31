package com.ai_builder_hackathon.gttgtt.ui.screen.grouplist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import com.ai_builder_hackathon.gttgtt.ui.component.AddActionCard
import com.ai_builder_hackathon.gttgtt.ui.component.HomeBottomNavBar
import com.ai_builder_hackathon.gttgtt.ui.component.HomeTab
import com.ai_builder_hackathon.gttgtt.ui.component.SearchField
import com.ai_builder_hackathon.gttgtt.ui.component.SmoothCornerShape
import com.ai_builder_hackathon.gttgtt.ui.component.hazeBackdrop
import com.ai_builder_hackathon.gttgtt.ui.component.rememberAppHazeState
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeBlue
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.component.AlbumLoader
import com.ai_builder_hackathon.gttgtt.ui.component.bounceClick
import com.ai_builder_hackathon.gttgtt.ui.theme.DisplayFontFamily
import com.ai_builder_hackathon.gttgtt.ui.theme.CardBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.DarkCircleButton
import com.ai_builder_hackathon.gttgtt.ui.theme.GlassTopHighlight
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackgroundBrush
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextMuted
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// 시안 .pad / .search / .list 의 좌우 여백은 모두 20
private val ScreenPadding = 20.dp
private const val VISIBLE_AVATAR_COUNT = 3
private val DangerColor = Color(0xFFB64B39)

// 카드: 그룹 피드 게시물 박스와 동일한 코너(RoundedCornerShape 24) + 파란빛 옅은 그림자.
private val CardCorner = 24.dp
private val SoftCardShadow = Color(0x1A4A6FA5)

// 상단 크롬(헤더+검색)이 목록 위에 겹쳐 뜨므로, 목록/빈 상태는 이만큼 아래에서 시작한다.
// 검색바와 첫 카드 사이 여백을 조금 넓혔다.
private val ChromeReservedHeight = 146.dp

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
    onCreateMemoryFromCamera: (archiveId: String, photoUri: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    // 카메라 촬영 → 기억 작성. TakePicture 는 FileProvider 로 만든 출력 URI 에 사진을 쓴다.
    // 촬영 후 그룹이 하나면 바로, 여러 개면 그룹 선택을 거쳐 기억 작성 화면으로 넘긴다.
    // cameraOutputUri 는 촬영 콜백에서만 쓰고 버리는 임시값이라 remember 로 충분하다.
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = cameraOutputUri
        if (success && uri != null) {
            val groups = viewModel.uiState.value.groups
            when {
                groups.isEmpty() ->
                    Toast.makeText(context, "먼저 그룹을 만들어 주세요.", Toast.LENGTH_SHORT).show()
                groups.size == 1 -> onCreateMemoryFromCamera(groups.first().id, uri.toString())
                else -> pendingCameraPhotoUri = uri.toString()
            }
        }
    }

    GroupListContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onGroupClick = onGroupClick,
        onProfileClick = onProfileClick,
        onCameraClick = {
            val uri = createCameraImageUri(context)
            cameraOutputUri = uri
            cameraLauncher.launch(uri)
        },
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

    // 촬영 후 그룹이 여러 개일 때 어느 그룹에 남길지 고르는 다이얼로그.
    pendingCameraPhotoUri?.let { photoUri ->
        CameraGroupPickerDialog(
            groups = uiState.groups,
            onPick = { archiveId ->
                pendingCameraPhotoUri = null
                onCreateMemoryFromCamera(archiveId, photoUri)
            },
            onDismiss = { pendingCameraPhotoUri = null },
        )
    }
}

@Composable
private fun GroupListContent(
    uiState: GroupListUiState,
    onQueryChange: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onCameraClick: () -> Unit = {},
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
    // 유리(Haze)의 배경 원본과 그 위에 뜨는 유리 요소가 같은 상태를 공유한다.
    val hazeState = rememberAppHazeState()
    var showAddCard by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // ── 1) 배경 + 스크롤 콘텐츠 = 유리에 비치는 원본(blur source) ──
        // 목록이 상단 크롬 밑으로 스크롤되며 검색바/네비바에 블러로 비친다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackgroundBrush)
                .hazeBackdrop(hazeState),
        ) {
            when {
                uiState.isLoading -> LoadingState(topInset = ChromeReservedHeight)
                uiState.errorMessage != null ->
                    MessageState(uiState.errorMessage, topInset = ChromeReservedHeight)
                uiState.isEmpty -> MessageState(
                    if (uiState.query.isBlank()) "아직 참여 중인 채팅방이 없어요." else "검색 결과가 없어요.",
                    topInset = ChromeReservedHeight,
                )

                else -> GroupList(
                    groups = uiState.groups,
                    onGroupClick = onGroupClick,
                    onGroupSettingsClick = onGroupSettingsClick,
                    topPadding = ChromeReservedHeight,
                )
            }
        }

        // ── 2) 상단 크롬(헤더 + 검색 + 배너) — 유리 자식(뒤 목록이 비쳐 블러됨) ──
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
        ) {
            HomeHeader(
                onProfileClick = onProfileClick,
                onAddClick = { showAddCard = !showAddCard },
                modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 6.dp, bottom = 2.dp),
            )
            SearchField(
                query = uiState.query,
                onQueryChange = onQueryChange,
                placeholder = "채팅방 검색",
                hazeState = hazeState,
                modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp),
            )
            if (uiState.updatingCoverImageArchiveId != null) {
                CoverImageStatusBanner(
                    text = "그룹 사진을 올리는 중이에요…",
                    isError = false,
                    onDismiss = null,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
                )
            } else if (uiState.coverImageError != null) {
                CoverImageStatusBanner(
                    text = uiState.coverImageError,
                    isError = true,
                    onDismiss = onDismissCoverImageError,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
                )
            }
        }

        // ── 3) 하단 플로팅 세그먼트 — 카메라(촬영) / 그룹(현재 화면) ──
        HomeBottomNavBar(
            selected = HomeTab.GROUP,
            onCameraClick = onCameraClick,
            onGroupClick = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
        )

        // ── 4) '+' 다크 글래스 액션 카드 (헤더 + 버튼으로 토글) ──
        if (showAddCard) {
            // 바깥을 누르면 닫히는 투명 스크림.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showAddCard = false },
            )
            AddActionCard(
                hazeState = hazeState,
                onCreateGroup = {
                    showAddCard = false
                    onCreateGroupClick()
                },
                onJoinByCode = {
                    showAddCard = false
                    onJoinByCodeClick()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = ScreenPadding)
                    .width(232.dp),
            )
        }
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
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
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
        // 우상단 [+][프로필] — 레이아웃 A. 다크 그레이 원형(이미지 2 톤).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeaderIconButton(
                iconRes = R.drawable.ic_plus,
                contentDescription = "추가",
                size = 44.dp,
                onClick = onAddClick,
            )
            HeaderIconButton(
                iconRes = R.drawable.ic_user,
                contentDescription = "내 프로필",
                size = 44.dp,
                onClick = onProfileClick,
            )
        }
    }
}

/** 헤더 우측의 원형 아이콘 버튼(+·프로필 공용). 다크 그레이 원 + 흰 아이콘 + 상단 하이라이트. */
@Composable
private fun HeaderIconButton(
    iconRes: Int,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = SoftCardShadow, ambientColor = SoftCardShadow)
            .clip(CircleShape)
            .background(DarkCircleButton)
            .border(width = 1.dp, color = GlassTopHighlight, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = SurfaceWhite,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

@Composable
private fun GroupList(
    groups: List<ArchiveSummary>,
    onGroupClick: (String) -> Unit,
    onGroupSettingsClick: (String) -> Unit = {},
    topPadding: Dp = 0.dp,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            // 상단 크롬(헤더+검색) 밑에서 시작하되, 그 아래로 스크롤되며 유리에 비친다.
            top = topPadding,
            // 하단 플로팅 네비바(약 60 + 여백)에 마지막 카드가 가리지 않도록 넉넉히 비운다.
            bottom = 100.dp,
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
    // 그룹 피드 게시물 박스와 동일한 코너.
    val cardShape = RoundedCornerShape(CardCorner)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 흰 카드가 배경 위로 떠 보이도록 크고 부드러운 그림자 + 큰 라운드.
            .shadow(elevation = 12.dp, shape = cardShape, spotColor = SoftCardShadow, ambientColor = SoftCardShadow)
            .clip(cardShape)
            .background(SurfaceWhite)
            .bounceClick(onClick = onClick)
            .padding(14.dp),
    ) {
        // 카카오톡형: [썸네일] [이름/프리뷰/아바타] [시간(위)·안읽음(아래)]
        // 오른쪽 열을 카드 높이만큼 늘려(SpaceBetween) 시간은 위, 안 읽음 배지는 아래로 벌린다.
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            GroupThumbnail(
                theme = group.theme,
                imageUrl = group.coverImageUrl,
                initial = group.name,
            )

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
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = group.lastMessagePreview,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))

                val shownMembers = group.memberIds.take(VISIBLE_AVATAR_COUNT)
                GroupAvatarStack(
                    memberIds = shownMembers,
                    // "+N" 의 N 은 전체 인원에서 실제로 그린 아바타 수를 뺀 값이다.
                    hiddenCount = group.hiddenMemberCount(shownMembers.size),
                    avatarUrlById = group.memberAvatarUrls,
                    avatarPathById = group.memberAvatarPaths,
                )
            }

            // 오른쪽 세로 열 — 위: 설정(점 세개)+시간, 아래: 안 읽음 배지.
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    // 점 세개 = 그룹 설정. 카드 전체 탭(bounceClick) 위에 얹혀도 nested clickable 이
                    // 안쪽 우선이라 이것만 반응한다.
                    Icon(
                        painter = painterResource(R.drawable.ic_dots),
                        contentDescription = "그룹 설정",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onSettingsClick)
                            .padding(4.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatTime(group.lastActivityAtMillis),
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (group.unreadCount > 0) {
                    UnreadCountBadge(count = group.unreadCount)
                } else {
                    // 배지가 없어도 오른쪽 열 폭이 흔들리지 않도록 자리를 유지한다.
                    Spacer(Modifier.size(20.dp))
                }
            }
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
private fun LoadingState(topInset: Dp = 0.dp) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AlbumLoader(modifier = Modifier.padding(top = topInset + 24.dp))
    }
}

@Composable
private fun MessageState(message: String, topInset: Dp = 0.dp) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = topInset + 16.dp),
        )
    }
}

/**
 * 카메라 촬영 결과를 저장할 앱 캐시 파일의 FileProvider content URI 를 만든다.
 * TakePicture 가 이 URI 에 사진을 쓰고, 이후 업로드는 같은 앱이 contentResolver 로 읽는다.
 */
private fun createCameraImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("capture_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** 촬영 직후 그룹이 여러 개일 때, 사진을 어느 그룹에 남길지 고르는 다이얼로그. */
@Composable
private fun CameraGroupPickerDialog(
    groups: List<ArchiveSummary>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SmoothCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(20.dp),
        ) {
            Text(
                text = "어느 그룹에 남길까요?",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "방금 찍은 사진으로 기억을 만들어요.",
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))

            groups.forEach { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SmoothCornerShape(16.dp))
                        .clickable { onPick(group.id) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GroupThumbnail(
                        theme = group.theme,
                        imageUrl = group.coverImageUrl,
                        initial = group.name,
                    )
                    Text(
                        text = group.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
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
