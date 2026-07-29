package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage

interface ChatRepository {
    /** 오래된 것부터 시간순 */
    suspend fun getMessages(archiveId: String): Result<List<ChatMessage>>

    /** 보낸 메시지를 돌려준다(서버가 부여한 id·시각 포함). */
    suspend fun sendMessage(archiveId: String, text: String): Result<ChatMessage>

    /**
     * 피드의 기억(게시물)을 이 그룹의 채팅방에 공유한다.
     * [memoryId] 는 반드시 같은 archive 소속이어야 한다 — 다른 그룹 기억을 공유하는 UI는 없다.
     */
    suspend fun sendSharedMemory(archiveId: String, memoryId: String): Result<ChatMessage>
}
