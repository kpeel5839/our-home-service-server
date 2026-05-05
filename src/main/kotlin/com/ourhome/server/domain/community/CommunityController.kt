package com.ourhome.server.domain.community

import com.ourhome.server.config.ForbiddenException
import com.ourhome.server.config.NotFoundException
import com.ourhome.server.config.SecurityUtils
import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.notification.DiscordNotificationService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class CommunityController(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val memberRepository: MemberRepository,
    private val discordNotificationService: DiscordNotificationService,
    @Value("\${domain:localhost:3000}") private val domain: String
) {
    private fun postLink(postId: String) = "https://$domain/community/$postId"

    @Transactional(readOnly = true)
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

    @Transactional
    @PostMapping
    fun createPost(@RequestBody request: CreatePostRequest): ResponseEntity<PostResponse> {
        val authorId = SecurityUtils.currentMemberId()
        val invalidImage = request.images.find { url ->
            !url.startsWith("https://res.cloudinary.com/")
        }
        if (invalidImage != null) throw IllegalArgumentException("허용되지 않은 이미지 URL입니다")
        val post = Post(
            authorId = authorId,
            content = request.content,
            images = request.images.toMutableList()
        )
        val saved = postRepository.save(post)
        val authorName = memberRepository.findById(authorId).map { it.name }.orElse("가족")
        val imagePart = if (request.images.isNotEmpty()) " 📷 사진 ${request.images.size}장" else ""
        val preview = if (request.content.length > 50) request.content.take(50) + "..." else request.content
        discordNotificationService.sendToAllMembers("📝 ${authorName}님이 피드를 올렸어요$imagePart\n$preview\n${postLink(saved.id)}")
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    fun getPost(@PathVariable id: String): ResponseEntity<PostResponse> {
        val post = postRepository.findById(id).orElseThrow { NotFoundException("Post not found: $id") }
        val comments = commentRepository.findByPostId(id)
        return ResponseEntity.ok(post.toResponse(comments))
    }

    @Transactional
    @PatchMapping("/{id}/like")
    fun toggleLike(@PathVariable id: String): ResponseEntity<Map<String, List<String>>> {
        val memberId = SecurityUtils.currentMemberId()
        val post = postRepository.findById(id).orElseThrow { NotFoundException("Post not found: $id") }
        val wasLiked = post.likes.contains(memberId)
        if (wasLiked) {
            post.likes.remove(memberId)
        } else {
            post.likes.add(memberId)
                val liker = memberRepository.findById(memberId).map { it.name }.orElse("누군가")
            discordNotificationService.sendToAllMembers("❤️ ${liker}님이 피드에 좋아요를 눌렀어요\n${postLink(id)}")
        }
        postRepository.save(post)
        return ResponseEntity.ok(mapOf("likes" to post.likes.toList()))
    }

    @PostMapping("/{id}/comments")
    fun createComment(
        @PathVariable id: String,
        @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CommentResponse> {
        val authorId = SecurityUtils.currentMemberId()
        val post = postRepository.findById(id).orElseThrow { NotFoundException("Post not found: $id") }
        val comment = Comment(postId = id, authorId = authorId, content = request.content)
        val saved = commentRepository.save(comment)
        val commenterName = memberRepository.findById(authorId).map { it.name }.orElse("누군가")
        val preview = if (request.content.length > 40) request.content.take(40) + "..." else request.content
        discordNotificationService.sendToAllMembers("💬 ${commenterName}님이 피드에 댓글을 달았어요\n\"$preview\"\n${postLink(id)}")
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: String): ResponseEntity<Void> {
        val memberId = SecurityUtils.currentMemberId()
        val post = postRepository.findById(id).orElseThrow { NotFoundException("Post not found: $id") }
        if (post.authorId != memberId) throw ForbiddenException("본인이 작성한 게시글만 삭제할 수 있습니다")
        commentRepository.deleteByPostId(id)
        postRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    fun deleteComment(
        @PathVariable id: String,
        @PathVariable commentId: String
    ): ResponseEntity<Void> {
        val memberId = SecurityUtils.currentMemberId()
        val comment = commentRepository.findById(commentId).orElseThrow { NotFoundException("Comment not found: $commentId") }
        if (comment.postId != id) throw NotFoundException("Comment does not belong to this post")
        if (comment.authorId != memberId) throw ForbiddenException("본인이 작성한 댓글만 삭제할 수 있습니다")
        commentRepository.deleteById(commentId)
        return ResponseEntity.noContent().build()
    }
}
