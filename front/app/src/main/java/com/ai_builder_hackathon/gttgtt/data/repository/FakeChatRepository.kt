package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.repository.ChatRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ⚠️ 임시 구현. Supabase 스키마가 올라가면 SupabaseChatRepository 로 교체하고 이 파일은 지운다.
 *
 * 보낸 메시지가 실제로 목록에 쌓이는지 확인하려고 메모리에 들고 있는다.
 * 앱을 재시작하면 초기화된다.
 */
@Singleton
class FakeChatRepository @Inject constructor() : ChatRepository {

    private val messages = seedMessages().toMutableList()

    override suspend fun getMessages(archiveId: String): Result<List<ChatMessage>> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        return Result.success(
            messages.filter { it.archiveId == archiveId }.sortedBy { it.sentAtMillis }
        )
    }

    override suspend fun sendMessage(archiveId: String, text: String): Result<ChatMessage> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("빈 메시지는 보낼 수 없습니다."))
        }

        val sent = ChatMessage(
            id = UUID.randomUUID().toString(),
            archiveId = archiveId,
            senderId = ME_ID,
            senderName = "나",
            sentAtMillis = System.currentTimeMillis(),
            text = trimmed,
            isMine = true,
        )
        messages += sent
        return Result.success(sent)
    }

    private fun seedMessages(): List<ChatMessage> = listOf(
        message(
            id = "m1",
            senderId = "u-minji",
            senderName = "민지",
            at = timeOn(2025, 12, 22, 18, 31),
            text = "여러분! 저 밤에 치킨 먹다 울었던 사진 찾아봤는데 너무 웃겨 ㅋㅋ",
        ),
        message(
            id = "m2",
            senderId = ME_ID,
            senderName = "나",
            at = timeOn(2025, 12, 22, 18, 33),
            text = "ㅋㅋㅋㅋㅋ 진짜 추억이다 그때",
            mine = true,
        ),
        message(
            id = "m3",
            senderId = "u-hyunwoo",
            senderName = "현우",
            at = timeOn(2025, 12, 22, 18, 33),
            text = "나 그때 좀 울었지.. ㅋㅋ",
        ),
        message(
            id = "m4",
            senderId = "u-jihun",
            senderName = "지훈",
            at = timeOn(2025, 12, 22, 18, 34),
            photo = GradientTheme.FOOD,
        ),
        message(
            id = "m5",
            senderId = ME_ID,
            senderName = "나",
            at = timeOn(2025, 12, 22, 18, 34),
            text = "이거 진짜 레전드 사진 ㅋㅋ",
            mine = true,
        ),
    )

    private fun message(
        id: String,
        senderId: String,
        senderName: String,
        at: Long,
        text: String? = null,
        photo: GradientTheme? = null,
        mine: Boolean = false,
    ) = ChatMessage(
        id = id,
        archiveId = DEMO_ARCHIVE_ID,
        senderId = senderId,
        senderName = senderName,
        sentAtMillis = at,
        text = text,
        photo = photo,
        isMine = mine,
    )

    private fun timeOn(y: Int, m: Int, d: Int, hour: Int, minute: Int): Long =
        LocalDate.of(y, m, d)
            .atTime(LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 300L

        /** FakeArchiveRepository 의 강릉 여행 그룹 id 와 맞춘다. */
        const val DEMO_ARCHIVE_ID = "archive-gangneung"
        const val ME_ID = "u-me"
    }
}
