package com.ourhome.server.domain.auth

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "kakao_users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["kakao_id"])]
)
class KakaoUser(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "kakao_id", nullable = false)
    val kakaoId: String,

    var nickname: String,
    var profileImageUrl: String? = null,
    var memberId: String? = null
)

data class KakaoLoginRequest(
    val code: String
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
