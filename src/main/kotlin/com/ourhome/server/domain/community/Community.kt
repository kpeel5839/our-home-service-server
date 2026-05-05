package com.ourhome.server.domain.community

import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "posts")
class Post(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val authorId: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_images", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "image_url")
    @BatchSize(size = 30)
    val images: MutableList<String> = mutableListOf(),

    @Column(nullable = false)
    val createdAt: String = Instant.now().toString(),

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_likes", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "member_id")
    @BatchSize(size = 30)
    val likes: MutableList<String> = mutableListOf()
)

@Entity
@Table(name = "comments")
class Comment(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val postId: String,

    @Column(nullable = false)
    val authorId: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false)
    val createdAt: String = Instant.now().toString()
)

data class PostResponse(
    val id: String, val authorId: String, val content: String,
    val images: List<String>, val createdAt: String, val likes: List<String>,
    val comments: List<CommentResponse> = emptyList()
)

data class CommentResponse(
    val id: String, val postId: String, val authorId: String,
    val content: String, val createdAt: String
)

data class PostListResponse(
    val posts: List<PostResponse>,
    val nextCursor: String?
)

data class CreatePostRequest(val content: String, val images: List<String> = emptyList())
data class CreateCommentRequest(val content: String)

fun Comment.toResponse() = CommentResponse(id, postId, authorId, content, createdAt)

fun Post.toResponse(comments: List<Comment> = emptyList()) = PostResponse(
    id, authorId, content, images.toList(), createdAt, likes.toList(), comments.map { it.toResponse() }
)
