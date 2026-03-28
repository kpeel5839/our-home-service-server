package com.ourhome.server.domain.auth

import com.ourhome.server.common.ErrorResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val kakaoAuthService: KakaoAuthService,
    private val kakaoUserRepository: KakaoUserRepository,
    @Value("\${admin.secret-key:change-me-admin-secret}") private val adminSecretKey: String
) {

    // ─── 인증 (public) ──────────────────────────────────────────────────────────

    /**
     * 카카오 인가 코드로 로그인
     * 응답의 status 에 따라 프론트에서 분기:
     *   - "APPROVED"  → accessToken 사용
     *   - "PENDING"   → /api/auth/status/{kakaoId} 폴링
     *   - 403         → 허용되지 않은 사용자 or 거절됨
     */
    @PostMapping("/kakao")
    fun kakaoLogin(@RequestBody request: KakaoLoginRequest): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(kakaoAuthService.login(request.code, request.redirectUri))
        } catch (e: IllegalStateException) {
            val (status, code, message) = when (e.message) {
                "LOGIN_NOT_ALLOWED" -> Triple(HttpStatus.FORBIDDEN, "NOT_ALLOWED", "허용되지 않은 사용자입니다.")
                "LOGIN_REJECTED"    -> Triple(HttpStatus.FORBIDDEN, "REJECTED", "관리자에 의해 거절되었습니다.")
                else                -> Triple(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR", e.message ?: "오류가 발생했습니다.")
            }
            ResponseEntity.status(status).body(ErrorResponse(message, code))
        }
    }

    /**
     * 승인 상태 폴링 — PENDING 상태 유저가 주기적으로 호출
     * APPROVED 가 되면 accessToken 포함하여 반환
     */
    @GetMapping("/status/{kakaoId}")
    fun getStatus(@PathVariable kakaoId: String): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(kakaoAuthService.checkStatus(kakaoId))

    // ─── 인증 유저 ──────────────────────────────────────────────────────────────

    /** 현재 로그인한 유저 정보 조회 */
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal claims: JwtClaims): ResponseEntity<MeResponse> {
        val user = kakaoUserRepository.findById(claims.kakaoId)
            .orElseThrow { IllegalArgumentException("User not found") }
        return ResponseEntity.ok(MeResponse(
            kakaoId = user.kakaoId,
            memberId = user.memberId,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl
        ))
    }

    /** 카카오 계정을 가족 구성원(FamilyMember)에 연결 */
    @PostMapping("/link-member")
    fun linkMember(
        @AuthenticationPrincipal claims: JwtClaims,
        @RequestBody request: LinkMemberRequest
    ): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(kakaoAuthService.linkMember(claims.kakaoId, request.memberId))

    // ─── 어드민 (X-Admin-Key 헤더 필요) ─────────────────────────────────────────

    /** 전체 승인 목록 조회 */
    @GetMapping("/approvals")
    fun getApprovals(
        @RequestHeader("X-Admin-Key") adminKey: String,
        @RequestParam(defaultValue = "pending") filter: String   // "pending" | "all"
    ): ResponseEntity<*> {
        verifyAdminKey(adminKey)?.let { return it }
        val list = if (filter == "all") kakaoAuthService.getAllApprovals()
                   else kakaoAuthService.getPendingApprovals()
        return ResponseEntity.ok(list)
    }

    /** 승인 */
    @PatchMapping("/approvals/{kakaoId}/approve")
    fun approve(
        @RequestHeader("X-Admin-Key") adminKey: String,
        @PathVariable kakaoId: String
    ): ResponseEntity<*> {
        verifyAdminKey(adminKey)?.let { return it }
        return ResponseEntity.ok(kakaoAuthService.approve(kakaoId))
    }

    /** 거절 */
    @PatchMapping("/approvals/{kakaoId}/reject")
    fun reject(
        @RequestHeader("X-Admin-Key") adminKey: String,
        @PathVariable kakaoId: String
    ): ResponseEntity<*> {
        verifyAdminKey(adminKey)?.let { return it }
        return ResponseEntity.ok(kakaoAuthService.reject(kakaoId))
    }

    private fun verifyAdminKey(key: String): ResponseEntity<ErrorResponse>? {
        if (key != adminSecretKey) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse("어드민 키가 올바르지 않습니다.", "FORBIDDEN"))
        }
        return null
    }
}
