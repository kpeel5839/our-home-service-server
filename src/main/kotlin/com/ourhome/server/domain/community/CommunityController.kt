package com.ourhome.server.domain.community

import com.ourhome.server.config.NotFoundException
import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.notification.DiscordNotificationService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class CommunityController(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val memberRepository: MemberRepository,
    private val discordNotificationService: DiscordNotificationService
) {

    @GetMapping
    fun getPosts(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<PostListResponse> {
        val pageable = PageRequest.of(0, limit + 1)
        val posts = if (cursor != null) {
            val cursorPost = postRepository.findById(cursor).orElseThrow { NotFoundException("Cursor post not found") }
            postRepository.findByCursorOrderByCreatedAtDesc(cursorPost.createdAt, pageable)
        } else {
            postRepository.findAllOrderByCreatedAtDesc(pageable)
        }
        val hasNext = posts.size > limit
        val resultPosts = if (hasNext) posts.dropLast(1) else posts
        val nextCursor = if (hasNext) resultPosts.last().id else null

        val postIds = resultPosts.map { it.id }
        val commentsByPost = commentRepository.findByPostIdIn(postIds).groupBy { it.postId }
        val responses = resultPosts.map { post ->
            post.toResponse(commentsByPost[post.id] ?: emptyList())
        }
        return ResponseEntity.ok(PostListResponse(responses, nextCursor))
    }

    @PostMapping
    fun createPost(@RequestBody request: CreatePostRequest): ResponseEntity<PostResponse> {
        val post = Post(
            authorId = request.authorId,
            content = request.content,
            images = request.images.toMutableList()
        )
        val saved = postRepository.save(post)
        val authorName = memberRepository.findById(request.authorId).map { it.name }.orElse("가족")
        val imagePart = if (request.images.isNotEmpty()) " 📷 사진 ${request.images.size}장" else ""
        val preview = if (request.content.length > 50) request.content.take(50) + "..." else request.content
        discordNotificationService.sendToAllMembers("📝 ${authorName}님이 피드를 올렸어요$imagePart\n$preview")
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping("/{id}")
    fun getPost(@PathVariable id: String): ResponseEntity<PostResponse> {
        val post = postRepository.findById(id).orElseThrow { NotFoundException("Post not found: $id") }
        val comments = commentRepository.findByPostId(id)
        return ResponseEntity.ok(post.toResponse(comments))
    }

    @PatchMapping("/{id}/like")
    fun toggleLike(
        @PathVariable id: String,
        @RequestBody request: LikeRequest
    ): ResponseEntity<Map<String, List<String>>> {
        val post = postRepository.findById(id).orElseThrow { NotFoundException("Post not found: $id") }
        if (post.likes.contains(request.memberId)) {
            post.likes.remove(request.memberId)
        } else {
            post.likes.add(request.memberId)
        }
        postRepository.save(post)
        return ResponseEntity.ok(mapOf("likes" to post.likes.toList()))
    }

    @PostMapping("/{id}/comments")
    fun createComment(
        @PathVariable id: String,
        @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CommentResponse> {
        if (!postRepository.existsById(id)) throw NotFoundException("Post not found: $id")
        val comment = Comment(postId = id, authorId = request.authorId, content = request.content)
        return ResponseEntity.status(HttpStatus.CREATED).body(commentRepository.save(comment).toResponse())
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    fun deleteComment(
        @PathVariable id: String,
        @PathVariable commentId: String
    ): ResponseEntity<Void> {
        val comment = commentRepository.findById(commentId).orElseThrow { NotFoundException("Comment not found: $commentId") }
        if (comment.postId != id) throw NotFoundException("Comment does not belong to this post")
        commentRepository.deleteById(commentId)
        return ResponseEntity.noContent().build()
    }
}
