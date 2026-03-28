package com.ourhome.server.domain.auth

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
     * 카카오 인가 코드로 로그인 / 회원가입
     * 프론트에서 카카오 OAuth 리다이렉트 후 code 를 백엔드로 전달
     */
    @PostMapping("/kakao")
    fun kakaoLogin(@RequestBody request: KakaoLoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(kakaoAuthService.login(request.code, request.redirectUri))

    /**
     * 카카오 계정을 가족 구성원(FamilyMember)에 연결
     * memberId 가 null 인 신규 유저가 구성원 선택 후 호출
     */
    @PostMapping("/link-member")
    fun linkMember(
        @AuthenticationPrincipal claims: JwtClaims,
        @RequestBody request: LinkMemberRequest
    ): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(kakaoAuthService.linkMember(claims.kakaoId, request.memberId))

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
