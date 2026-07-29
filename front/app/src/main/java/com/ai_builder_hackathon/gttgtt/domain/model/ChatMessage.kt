package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 그룹 채팅의 메시지 하나. (멤버들끼리의 대화이며, AI 대화는 별도다.)
 *
 * 사진은 [Photo] — signed URL 이 없으면 그라디언트 fallback 으로 그려진다.
 */
data class ChatMessage(
    val id: String,
    val archiveId: String,
    val senderId: String,
    val senderName: String,
    val sentAtMillis: Long,
    val text: String? = null,
    val photo: Photo? = null,
    /** 피드에서 "보내기"로 공유한 기억. 텍스트/사진 없이 이것만 있는 메시지도 가능하다. */
    val sharedMemory: SharedMemoryPreview? = null,
    /** 내가 보낸 메시지면 오른쪽에 그린 말풍선으로 그린다. */
    val isMine: Boolean = false,
) {
    init {
        require(text != null || photo != null || sharedMemory != null) {
            "메시지는 텍스트·사진·공유된 기억 중 하나는 있어야 한다."
        }
    }

    val isPhoto: Boolean get() = photo != null
    val isSharedMemory: Boolean get() = sharedMemory != null
}

/**
 * 채팅 말풍선에 보여줄 만큼만 담은 공유된 기억 미리보기.
 * [Post]/[MemoryDetail] 전체를 끌고 오지 않는 이유는 채팅 목록이 N개 메시지를 한 번에
 * 그리므로 꼭 필요한 필드만 남겨야 조회 비용이 커지지 않기 때문이다.
 */
data class SharedMemoryPreview(
    val memoryId: String,
    val title: String,
    val photo: Photo?,
    val memoryDateMillis: Long,
)

/**
 * 화면에 그릴 단위. 날짜가 바뀌는 지점에 구분선이 들어가야 해서
 * 메시지 목록을 그대로 쓰지 않고 이 타입으로 변환한다.
 *
 * 이 조립을 ViewModel 에서 해두면 Composable 은 분기 없이 그리기만 하면 된다.
 */
sealed interface ChatListItem {
    val key: String

    data class DateHeader(val dateMillis: Long) : ChatListItem {
        override val key: String get() = "date-$dateMillis"
    }

    data class Message(val message: ChatMessage) : ChatListItem {
        override val key: String get() = "msg-${message.id}"
    }
}
