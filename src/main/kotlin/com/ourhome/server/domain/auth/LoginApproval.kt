package com.ourhome.server.domain.auth

import jakarta.persistence.*
import java.time.Instant

enum class ApprovalStatus { PENDING, APPROVED, REJECTED }

@Entity
@Table(name = "login_approvals")
class LoginApproval(
    @Id
    val kakaoId: String,

    val nickname: String,
    val profileImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ApprovalStatus = ApprovalStatus.PENDING,

    @Column(nullable = false)
    val requestedAt: String = Instant.now().toString(),

    var processedAt: String? = null
)

data class LoginResponse(
    val status: String,               // "PENDING" | "APPROVED" | "REJECTED"
    val kakaoId: String,
    val accessToken: String? = null,  // APPROVED 일 때만 포함
    val memberId: String? = null,
    val nickname: String,
    val profileImageUrl: String?
)

data class ApprovalItemResponse(
    val kakaoId: String,
    val nickname: String,
    val profileImageUrl: String?,
    val status: String,
    val requestedAt: String,
    val processedAt: String?
)

fun LoginApproval.toResponse() = ApprovalItemResponse(
    kakaoId, nickname, profileImageUrl, status.name, requestedAt, processedAt
)
