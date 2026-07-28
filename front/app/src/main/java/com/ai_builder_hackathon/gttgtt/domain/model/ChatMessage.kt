package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 그룹 채팅의 메시지 하나. (멤버들끼리의 대화이며, AI 대화는 별도다.)
 *
 * 사진은 Storage 가 붙기 전까지 [GradientTheme] 플레이스홀더로 둔다.
 */
data class ChatMessage(
    val id: String,
    val archiveId: String,
    val senderId: String,
    val senderName: String,
    val sentAtMillis: Long,
    val text: String? = null,
    val photo: GradientTheme? = null,
    /** 내가 보낸 메시지면 오른쪽에 그린 말풍선으로 그린다. */
    val isMine: Boolean = false,
) {
    init {
        require(text != null || photo != null) { "메시지는 텍스트나 사진 중 하나는 있어야 한다." }
    }

    val isPhoto: Boolean get() = photo != null
}

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
