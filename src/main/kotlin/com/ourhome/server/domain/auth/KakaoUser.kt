package com.ourhome.server.domain.auth

import jakarta.persistence.*

@Entity
@Table(name = "kakao_users")
class KakaoUser(
    @Id
    val kakaoId: String,

    var nickname: String,
    var profileImageUrl: String? = null,
    var memberId: String? = null
)

data class KakaoLoginRequest(
    val code: String,
    val redirectUri: String
)

data class AuthResponse(
    val accessToken: String,
    val memberId: String,
    val nickname: String,
    val profileImageUrl: String?
)

data class MeResponse(
    val kakaoId: String,
    val memberId: String?,
    val nickname: String,
    val profileImageUrl: String?
)

data class LinkMemberRequest(val memberId: String)
