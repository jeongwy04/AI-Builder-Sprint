package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.Post

interface PostRepository {
    /** 고정 게시물이 먼저, 그다음 추억 날짜 최신순 */
    suspend fun getFeed(archiveId: String): Result<List<Post>>

    /** 좋아요 토글. 갱신된 게시물을 돌려준다. */
    suspend fun toggleLike(postId: String): Result<Post>
}
