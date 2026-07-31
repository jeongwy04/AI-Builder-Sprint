package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.ChatRequest
import com.ai_builder_hackathon.gttgtt.data.dto.ChatResponse
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryCardDto
import com.ai_builder_hackathon.gttgtt.domain.model.AiMessage
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryHit
import com.ai_builder_hackathon.gttgtt.domain.repository.AiChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * chat Edge Function 을 통한 AI 추억 검색 실구현.
 *
 * 앱은 Upstage 를 직접 부르지 않는다 — 키가 서버에만 있기 때문 (CLAUDE.md §5.4).
 * 응답의 memory_ids 로 memories 를 조회해 결과 카드를 만든다.
 * LLM 은 이 조회 결과 안에서만 답한다는 전제가 서버 프롬프트에 걸려 있다 (§6.3).
 */
@Singleton
class SupabaseAiChatRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : AiChatRepository {

    /** 세션은 대화 맥락 유지용. 화면을 벗어나도 이어가도록 서버 session_id 를 기억한다. */
    private var sessionId: String? = null

    private val json = Json { ignoreUnknownKeys = true }

    override fun greeting() = AiMessage(
        id = "greeting",
        role = AiMessage.Role.ASSISTANT,
        text = "어떤 추억을 찾고 싶으신가요?",
    )

    override suspend fun send(archiveId: String, text: String): Result<AiMessage> = runCatching {
        val response = invokeChatWithRetry(archiveId, text)
        val chat = json.decodeFromString<ChatResponse>(response.bodyAsText())
        sessionId = chat.sessionId

        val hits = if (chat.memoryIds.isEmpty()) emptyList() else fetchHits(chat.memoryIds)

        AiMessage(
            id = UUID.randomUUID().toString(),
            role = AiMessage.Role.ASSISTANT,
            // 폴백(키워드) 검색이면 정확도가 낮을 수 있음을 사용자에게 알린다 (§9).
            text = if (chat.degraded == true) {
                "${chat.reply}\n(연결이 원활하지 않아 키워드로 찾았어요)"
            } else {
                chat.reply
            },
            results = hits,
        )
    }.recoverCatching { throwable ->
        // 타임아웃(20초, SupabaseModule 설정) 을 넘기고도 실패하면 여기까지 온다 — 원본 예외의
        // 기술적인 문구("request timeout has expired [...]", "Socket timeout has expired [...]")를
        // 그대로 보여주지 않고 사용자가 이해할 수 있는 문구로 바꾼다.
        if (throwable is HttpRequestTimeoutException || throwable is SocketTimeoutException) {
            error("서버 응답이 늦어지고 있어요. 잠시 후 다시 시도해주세요.")
        }
        throw throwable
    }

    /**
     * `chat` 호출은 콜드스타트 + Solar Pro 3 function calling 왕복이 겹치면 오래 걸려서
     * 가끔 실패한다. 무한 재시도는 금지고(CLAUDE.md §8.2), 딱 1번만 더 시도한다.
     * 코루틴 취소(CancellationException)는 재시도 대상이 아니라 그대로 던진다.
     */
    private suspend fun invokeChatWithRetry(archiveId: String, text: String): HttpResponse {
        val request = ChatRequest(archiveId = archiveId, message = text, sessionId = sessionId)
        return try {
            supabase.functions.invoke(function = "chat", body = request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            supabase.functions.invoke(function = "chat", body = request)
        }
    }

    private suspend fun fetchHits(memoryIds: List<String>): List<MemoryHit> {
        val cards = supabase.postgrest.from("memories")
            // embedding 컬럼은 4096차원 벡터라 절대 select 하지 않는다.
            .select(Columns.raw("id,memory_date,search_text")) {
                filter { isIn("id", memoryIds) }
            }
            .decodeList<MemoryCardDto>()

        // 서버가 유사도순으로 준 id 순서를 유지한다.
        val byId = cards.associateBy { it.id }
        return memoryIds.mapNotNull { id -> byId[id]?.toHit() }
    }

    private fun MemoryCardDto.toHit() = MemoryHit(
        memoryId = id,
        title = searchText
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.take(TITLE_MAX)
            ?: "제목 없는 기억",
        memoryDateMillis = parseDateMillis(memoryDate),
        // 대표 사진 signed URL 은 media_assets 연동 시 채운다. 지금은 id 해시 fallback.
        thumbnail = Photo(
            fallback = GradientTheme.entries[id.hashCode().absoluteValue % GradientTheme.entries.size],
        ),
    )

    private fun parseDateMillis(date: String): Long =
        runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(0L)

    private companion object {
        const val TITLE_MAX = 40
    }
}
