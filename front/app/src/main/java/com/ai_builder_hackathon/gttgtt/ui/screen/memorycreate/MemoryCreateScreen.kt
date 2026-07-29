package com.ai_builder_hackathon.gttgtt.ui.screen.memorycreate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.Participant
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import com.ai_builder_hackathon.gttgtt.ui.component.MemberAvatar
import com.ai_builder_hackathon.gttgtt.ui.component.PhotoImage
import com.ai_builder_hackathon.gttgtt.ui.component.TopBarButton
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenSoft
import com.ai_builder_hackathon.gttgtt.ui.theme.CardBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextMuted
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import com.ai_builder_hackathon.gttgtt.ui.theme.TopBarIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SidePadding = 20.dp
private const val MAX_PHOTOS = 10

@Composable
fun MemoryCreateScreen(
    onBackClick: () -> Unit,
    onSaved: (memoryId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryCreateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 저장 성공 시 컨페티를 터뜨리고(이펙트 4), 잠깐 보여준 뒤 화면을 나간다.
    var confetti by remember { mutableStateOf(0) }
    LaunchedEffect(uiState.savedMemoryId) {
        val id = uiState.savedMemoryId ?: return@LaunchedEffect
        confetti += 1
        kotlinx.coroutines.delay(900)
        onSaved(id)
    }

    // 사진 선택. Photo Picker 는 저장소 권한을 요구하지 않는다.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
    ) { uris ->
        viewModel.onPhotosPicked(uris.map { it.toString() })
    }

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        MemoryCreateContent(
            uiState = uiState,
            onBackClick = onBackClick,
            onPickPhotos = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPhotoRemove = viewModel::onPhotoRemove,
            onExistingPhotoRemove = viewModel::onExistingPhotoRemove,
            onTitleChange = viewModel::onTitleChange,
            onBodyChange = viewModel::onBodyChange,
            onDateChange = viewModel::onDateChange,
            onParticipantToggle = viewModel::onParticipantToggle,
            onSaveClick = viewModel::onSaveClick,
        )
        com.ai_builder_hackathon.gttgtt.ui.component.ConfettiOverlay(trigger = confetti)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryCreateContent(
    uiState: MemoryCreateUiState,
    onBackClick: () -> Unit,
    onPickPhotos: () -> Unit,
    onPhotoRemove: (String) -> Unit,
    onExistingPhotoRemove: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onParticipantToggle: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDatePickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        CreateTopBar(
            title = if (uiState.isEditMode) "기억 수정" else "기억 남기기",
            canSave = uiState.canSave,
            isSaving = uiState.isSaving,
            onBackClick = onBackClick,
            onSaveClick = onSaveClick,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            PhotoStrip(
                existingPhotos = uiState.visibleExistingPhotos,
                photoUris = uiState.photoUris,
                onPickPhotos = onPickPhotos,
                onPhotoRemove = onPhotoRemove,
                onExistingPhotoRemove = onExistingPhotoRemove,
            )

            Spacer(Modifier.height(20.dp))
            DateChip(
                millis = uiState.memoryDateMillis,
                isFromExif = uiState.isDateFromExif,
                onClick = { isDatePickerOpen = true },
            )

            Spacer(Modifier.height(20.dp))
            FieldLabel("제목")
            PlainTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                placeholder = "비워두면 본문 첫 줄이 제목이 돼요",
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))
            FieldLabel("이야기")
            PlainTextField(
                value = uiState.body,
                onValueChange = onBodyChange,
                placeholder = "그날 어땠는지 적어주세요. 나중에 AI가 이 문장으로 찾아줘요.",
                singleLine = false,
            )

            // 사진만 있고 메모가 없으면 검색 근거가 빈약해진다 (CLAUDE.md §1, §6.4).
            if (uiState.shouldSuggestNote) {
                Spacer(Modifier.height(10.dp))
                NoteHint()
            }

            if (uiState.members.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                FieldLabel("함께한 사람")
                ParticipantPicker(
                    members = uiState.members,
                    selectedIds = uiState.selectedParticipantIds,
                    onToggle = onParticipantToggle,
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = SidePadding),
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (isDatePickerOpen) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.memoryDateMillis)
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(onDateChange)
                    isDatePickerOpen = false
                }) { Text("확인", color = BrandGreenDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerOpen = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CreateTopBar(
    title: String,
    canSave: Boolean,
    isSaving: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SidePadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopBarButton(
            iconRes = R.drawable.ic_x,
            contentDescription = "닫기",
            background = SurfaceWhite,
            tint = TopBarIcon,
            onClick = onBackClick,
        )
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
        )
        SaveButton(canSave = canSave, isSaving = isSaving, onClick = onSaveClick)
    }
}

@Composable
private fun SaveButton(canSave: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            // GroupChatScreen 등과 같은 이유로 alpha() 대신 색 자체에 투명도를 넣는다 —
            // 그렇지 않으면 일부 기기에서 버튼이 하얗게(비활성 색이 안 먹은 채) 보인다.
            .background(if (canSave) BrandGreen else BrandGreen.copy(alpha = 0.4f))
            .clickable(enabled = canSave, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                color = SurfaceWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Text(
                text = "저장",
                color = SurfaceWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/**
 * 가로로 넘겨보는 사진 목록. 맨 앞이 추가 버튼.
 * 기존(서버에 이미 저장된) 사진과 새로 고른(아직 업로드 전) 사진을 함께 보여준다 —
 * 수정 모드에서도 둘 다 추가/삭제할 수 있다.
 */
@Composable
private fun PhotoStrip(
    existingPhotos: List<Photo>,
    photoUris: List<String>,
    onPickPhotos: () -> Unit,
    onPhotoRemove: (String) -> Unit,
    onExistingPhotoRemove: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SidePadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "add") {
            AddPhotoButton(count = existingPhotos.size + photoUris.size, onClick = onPickPhotos)
        }
        items(existingPhotos, key = { "existing-${it.id}" }) { photo ->
            val photoId = photo.id
            if (photoId != null) {
                ExistingPhotoThumbnail(photo = photo, onRemove = { onExistingPhotoRemove(photoId) })
            }
        }
        items(photoUris, key = { "new-$it" }) { uri ->
            PhotoThumbnail(uri = uri, onRemove = { onPhotoRemove(uri) })
        }
    }
}

@Composable
private fun AddPhotoButton(count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, BrandGreenSoft, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = "사진 추가",
            tint = BrandGreen,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$count / $MAX_PHOTOS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PhotoThumbnail(uri: String, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(100.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground),
        )
        PhotoRemoveButton(onRemove = onRemove)
    }
}

/** 이미 서버에 저장된 사진 하나. signed URL 이 아직 없으면 [PhotoImage] 가 그라디언트로 채운다. */
@Composable
private fun ExistingPhotoThumbnail(photo: Photo, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(100.dp)) {
        PhotoImage(
            photo = photo,
            corner = 18.dp,
            modifier = Modifier.fillMaxSize(),
        )
        PhotoRemoveButton(onRemove = onRemove)
    }
}

@Composable
private fun BoxScope.PhotoRemoveButton(onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .clickable(onClick = onRemove),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_x),
            contentDescription = "사진 제거",
            tint = TopBarIcon,
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun DateChip(millis: Long, isFromExif: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = SidePadding)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            tint = BrandGreenDark,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = formatDate(millis),
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        if (isFromExif) {
            // 사진에서 자동으로 읽어온 값임을 알려준다. 틀리면 눌러서 고칠 수 있다.
            Text(
                text = "촬영일 자동",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = SidePadding, end = SidePadding, bottom = 8.dp),
    )
}

@Composable
private fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = SidePadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = LocalTextStyle.current.copy(
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier else Modifier.heightIn(min = 96.dp)),
        )
    }
}

@Composable
private fun NoteHint() {
    Text(
        text = "메모를 남기면 나중에 AI가 이 기억을 찾아줄 수 있어요.",
        color = BrandGreenDark,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(horizontal = SidePadding)
            .clip(RoundedCornerShape(12.dp))
            .background(BrandGreenSoft)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun ParticipantPicker(
    members: List<Participant>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SidePadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(members, key = { it.id }) { member ->
            val isSelected = member.id in selectedIds
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToggle(member.id) },
            ) {
                Box {
                    MemberAvatar(
                        memberId = member.id,
                        size = 48.dp,
                        showRing = false,
                        modifier = Modifier.alpha(if (isSelected) 1f else 0.45f),
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(BrandGreen)
                                .border(2.dp, ScreenBackground, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = member.name,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateFormatter)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MemoryCreateContentPreview() {
    GttgttTheme {
        MemoryCreateContent(
            uiState = MemoryCreateUiState(
                title = "",
                body = "",
                photoUris = emptyList(),
                memoryDateMillis = 1_766_361_600_000L,
                isDateFromExif = true,
                members = listOf(
                    Participant("u-minji", "민지"),
                    Participant("u-hyunwoo", "현우"),
                    Participant("u-jihun", "지훈"),
                ),
                selectedParticipantIds = setOf("u-minji"),
            ),
            onBackClick = {},
            onPickPhotos = {},
            onPhotoRemove = {},
            onExistingPhotoRemove = {},
            onTitleChange = {},
            onBodyChange = {},
            onDateChange = {},
            onParticipantToggle = {},
            onSaveClick = {},
        )
    }
}
