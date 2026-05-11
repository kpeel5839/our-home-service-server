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

    @Column
    var updatedAt: String? = null,

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
    var content: String,

    @Column(nullable = false)
    val createdAt: String = Instant.now().toString(),

    @Column
    var updatedAt: String? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "comment_mentions", joinColumns = [JoinColumn(name = "comment_id")])
    @Column(name = "member_id")
    @BatchSize(size = 10)
    val mentions: MutableList<String> = mutableListOf(),

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "comment_likes", joinColumns = [JoinColumn(name = "comment_id")])
    @Column(name = "member_id")
    @BatchSize(size = 10)
    val likes: MutableList<String> = mutableListOf()
)

data class PostResponse(
    val id: String, val authorId: String, val content: String,
    val images: List<String>, val createdAt: String, val updatedAt: String?,
    val likes: List<String>, val comments: List<CommentResponse> = emptyList()
)

data class CommentResponse(
    val id: String, val postId: String, val authorId: String,
    val content: String, val createdAt: String, val updatedAt: String?,
    val mentions: List<String> = emptyList(),
    val likes: List<String> = emptyList()
)

data class PostListResponse(
    val posts: List<PostResponse>,
    val nextCursor: String?
)

data class CreatePostRequest(val content: String, val images: List<String> = emptyList())
data class UpdatePostRequest(val content: String, val images: List<String> = emptyList())
data class CreateCommentRequest(val content: String, val mentions: List<String> = emptyList())
data class UpdateCommentRequest(val content: String, val mentions: List<String> = emptyList())

fun Comment.toResponse() = CommentResponse(id, postId, authorId, content, createdAt, updatedAt, mentions.toList(), likes.toList())

fun Post.toResponse(comments: List<Comment> = emptyList()) = PostResponse(
    id, authorId, content, images.toList(), createdAt, updatedAt, likes.toList(), comments.map { it.toResponse() }
)
