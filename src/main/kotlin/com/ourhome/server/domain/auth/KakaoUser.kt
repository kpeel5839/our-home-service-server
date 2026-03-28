package com.ourhome.server.domain.auth

import jakarta.persistence.*

/**
 * 카카오 OAuth 로그인 사용자.
 * memberId 는 관리자가 가족 구성원(FamilyMember)과 연결한 뒤 채워집니다.
 */
@Entity
@Table(name = "kakao_users")
class KakaoUser(
    @Id
    val kakaoId: String,           // 카카오 회원번호 (고유)

    var nickname: String,
    var profileImageUrl: String? = null,

    /** 연결된 FamilyMember.id (m1~m4). null 이면 아직 매핑 안 된 상태 */
    var memberId: String? = null
)

data class KakaoLoginRequest(
    val code: String,
    val redirectUri: String
)

/** 로그인/링크 응답 — accessToken 포함 */
data class AuthResponse(
    val accessToken: String,
    val memberId: String?,        // null 이면 프론트에서 구성원 선택 화면으로 이동
    val nickname: String,
    val profileImageUrl: String?
)

/** GET /api/auth/me 응답 — 토큰 재발급 없음 */
data class MeResponse(
    val kakaoId: String,
    val memberId: String?,
    val nickname: String,
    val profileImageUrl: String?
)

data class LinkMemberRequest(val memberId: String)
