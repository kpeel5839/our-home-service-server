package com.ourhome.server.domain.auth

import com.ourhome.server.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val kakaoAuthService: KakaoAuthService,
    private val kakaoUserRepository: KakaoUserRepository
) {

    /**
     * 카카오 인가 코드로 로그인
     * 허용된 4명(김기필/윤재희/김재연/김정은) 만 접근 가능, 즉시 JWT 발급
     */
    @PostMapping("/kakao")
    fun kakaoLogin(@RequestBody request: KakaoLoginRequest): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(kakaoAuthService.login(request.code))
        } catch (e: IllegalStateException) {
            val (status, code, message) = when (e.message) {
                "LOGIN_NOT_ALLOWED" -> Triple(HttpStatus.FORBIDDEN, "NOT_ALLOWED", "가족 구성원만 로그인할 수 있습니다.")
                else -> Triple(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR", e.message ?: "오류가 발생했습니다.")
            }
            ResponseEntity.status(status).body(ErrorResponse(message, code))
        }
    }

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
}
