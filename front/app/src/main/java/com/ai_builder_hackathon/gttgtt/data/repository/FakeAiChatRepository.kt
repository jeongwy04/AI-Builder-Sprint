package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.model.AiMessage
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.asPhoto
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryHit
import com.ai_builder_hackathon.gttgtt.domain.repository.AiChatRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ⚠️ 임시 구현. `chat` Edge Function 이 올라가면 SupabaseAiChatRepository 로 교체하고 이 파일은 지운다.
 *
 * 실제 LLM 없이도 CLAUDE.md §6.4 의 대화 흐름을 그대로 재현한다.
 * 조건이 부족하면 되묻고, 조건이 잡히면 결과를 준다.
 */
@Singleton
class FakeAiChatRepository @Inject constructor() : AiChatRepository {

    override fun greeting() = AiMessage(
        id = "greeting",
        role = AiMessage.Role.ASSISTANT,
        text = "어떤 추억을 찾고 싶으신가요?",
    )

    override suspend fun send(archiveId: String, text: String): Result<AiMessage> {
        // LLM 응답을 기다리는 느낌을 주기 위한 지연. 실제 구현에서는 제거된다.
        delay(FAKE_THINKING_DELAY_MILLIS)

        val query = text.trim()
        if (query.isEmpty()) {
            return Result.failure(IllegalArgumentException("빈 메시지는 보낼 수 없습니다."))
        }

        // 조건이 부족하면 즉시 검색하지 않고 한 번 되묻는다 (§6.4).
        if (!query.hasSearchableCondition()) {
            return Result.success(
                assistant("언제쯤이었는지, 또는 어디에서 있었던 일인지 기억나세요? 조금만 좁혀주시면 찾아볼게요.")
            )
        }

        val hits = ALL_MEMORIES.filter { it.matches(query) }

        // 결과가 없으면 지어내지 말고 조건 완화를 제안한다 (§6.3).
        if (hits.isEmpty()) {
            return Result.success(
                assistant("그 조건으로는 찾지 못했어요. 기간을 넓혀보거나 다른 장소로 다시 물어봐 주세요.")
            )
        }

        return Result.success(
            assistant("이런 기억을 찾았어요.", hits)
        )
    }

    private fun assistant(text: String, results: List<MemoryHit> = emptyList()) = AiMessage(
        id = UUID.randomUUID().toString(),
        role = AiMessage.Role.ASSISTANT,
        text = text,
        results = results,
    )

    /** 기간·장소·주제 중 하나라도 걸리는 단어가 있는지 본다. */
    private fun String.hasSearchableCondition(): Boolean =
        CONDITION_HINTS.any { contains(it, ignoreCase = true) }

    private fun MemoryHit.matches(query: String): Boolean =
        KEYWORDS[memoryId].orEmpty().any { query.contains(it, ignoreCase = true) }

    private companion object {
        const val FAKE_THINKING_DELAY_MILLIS = 700L

        /** 이 단어들이 하나도 없으면 "조건 부족"으로 보고 되묻는다. */
        val CONDITION_HINTS = listOf(
            "작년", "재작년", "올해", "겨울", "여름", "봄", "가을",
            "월", "년", "바다", "강릉", "치킨", "여행", "숙소", "밤", "야식", "시험",
        )

        val KEYWORDS = mapOf(
            "mem-chicken" to listOf("치킨", "시험", "겨울", "12월", "울", "밤"),
            "mem-sea" to listOf("바다", "강릉", "여행", "겨울", "12월", "날씨"),
            "mem-night" to listOf("숙소", "야식", "밤", "새벽", "겨울", "12월"),
        )

        val ALL_MEMORIES = listOf(
            MemoryHit(
                memoryId = "mem-chicken",
                title = "시험 끝나고 치킨 먹다 울었던 날",
                memoryDateMillis = dateOf(2025, 12, 22),
                thumbnail = GradientTheme.FOOD.asPhoto(),
            ),
            MemoryHit(
                memoryId = "mem-sea",
                title = "바다 진짜 예뻤던 강릉 첫날",
                memoryDateMillis = dateOf(2025, 12, 21),
                thumbnail = GradientTheme.SEA.asPhoto(),
            ),
            MemoryHit(
                memoryId = "mem-night",
                title = "숙소에서 야식 먹으며 새벽까지",
                memoryDateMillis = dateOf(2025, 12, 20),
                thumbnail = GradientTheme.NIGHT.asPhoto(),
            ),
        )

        fun dateOf(y: Int, m: Int, d: Int): Long =
            LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
