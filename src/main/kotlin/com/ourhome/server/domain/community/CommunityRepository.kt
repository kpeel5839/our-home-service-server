package com.ourhome.server.domain.community

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostRepository : JpaRepository<Post, String> {
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    fun findAllOrderByCreatedAtDesc(pageable: Pageable): List<Post>

    @Query("SELECT p FROM Post p WHERE p.createdAt < :cursor ORDER BY p.createdAt DESC")
    fun findByCursorOrderByCreatedAtDesc(cursor: String, pageable: Pageable): List<Post>
}

interface CommentRepository : JpaRepository<Comment, String> {
    fun findByPostId(postId: String): List<Comment>
    fun findByPostIdIn(postIds: List<String>): List<Comment>
    fun deleteByPostId(postId: String)
}
