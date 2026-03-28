package com.ourhome.server.domain.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.ourhome.server.config.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

@Service
class KakaoAuthService(
    private val kakaoUserRepository: KakaoUserRepository,
    private val approvalRepository: LoginApprovalRepository,
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper,
    @Value("\${kakao.rest-api-key}") private val restApiKey: String,
    @Value("\${kakao.token-uri}") private val tokenUri: String,
    @Value("\${kakao.user-info-uri}") private val userInfoUri: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    companion object {
        /** 로그인 허용 이름 목록 (카카오 닉네임에 포함되어야 함) */
        private val ALLOWED_NAMES = listOf("김재연", "윤재희", "김기필", "김정은")

        /** 승인 없이 즉시 로그인되는 이름 */
        private const val AUTO_APPROVE_NAME = "김재연"
    }

    /**
     * 카카오 인가 코드로 로그인.
     * - 허용 이름 목록에 없으면 → IllegalStateException (403)
     * - "김재연" 포함 → 즉시 APPROVED + JWT 반환
     * - 그 외 최초 요청 → PENDING 생성 → { status: "PENDING" } 반환
     * - 관리자 승인 후 재요청 → { status: "APPROVED", accessToken } 반환
     * - 거절된 경우 → IllegalStateException (403)
     */
    fun login(code: String, redirectUri: String): LoginResponse {
        val kakaoToken = fetchKakaoToken(code, redirectUri)
        val userInfo = fetchKakaoUserInfo(kakaoToken)

        // 허용된 이름인지 확인
        val isAllowed = ALLOWED_NAMES.any { userInfo.nickname.contains(it) }
        if (!isAllowed) throw IllegalStateException("LOGIN_NOT_ALLOWED")

        // KakaoUser 생성 또는 정보 업데이트
        val user = kakaoUserRepository.findById(userInfo.id).orElseGet {
            kakaoUserRepository.save(KakaoUser(
                kakaoId = userInfo.id,
                nickname = userInfo.nickname,
                profileImageUrl = userInfo.profileImageUrl
            ))
        }.also { existing ->
            if (existing.nickname != userInfo.nickname || existing.profileImageUrl != userInfo.profileImageUrl) {
                existing.nickname = userInfo.nickname
                existing.profileImageUrl = userInfo.profileImageUrl
                kakaoUserRepository.save(existing)
            }
        }

        // 자동 승인 대상 (김재연)
        if (userInfo.nickname.contains(AUTO_APPROVE_NAME)) {
            val approval = approvalRepository.findById(userInfo.id).orElseGet {
                approvalRepository.save(LoginApproval(
                    kakaoId = userInfo.id,
                    nickname = userInfo.nickname,
                    profileImageUrl = userInfo.profileImageUrl,
                    status = ApprovalStatus.APPROVED,
                    processedAt = Instant.now().toString()
                ))
            }
            if (approval.status != ApprovalStatus.APPROVED) {
                approval.status = ApprovalStatus.APPROVED
                approval.processedAt = Instant.now().toString()
                approvalRepository.save(approval)
            }
            return LoginResponse(
                status = "APPROVED",
                kakaoId = user.kakaoId,
                accessToken = jwtService.generate(user.kakaoId, user.memberId),
                memberId = user.memberId,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl
            )
        }

        // 일반 사용자 — 승인 상태 확인
        val approval = approvalRepository.findById(userInfo.id).orElseGet {
            approvalRepository.save(LoginApproval(
                kakaoId = userInfo.id,
                nickname = userInfo.nickname,
                profileImageUrl = userInfo.profileImageUrl
            ))
        }

        return when (approval.status) {
            ApprovalStatus.APPROVED -> LoginResponse(
                status = "APPROVED",
                kakaoId = user.kakaoId,
                accessToken = jwtService.generate(user.kakaoId, user.memberId),
                memberId = user.memberId,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl
            )
            ApprovalStatus.PENDING -> LoginResponse(
                status = "PENDING",
                kakaoId = user.kakaoId,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl
            )
            ApprovalStatus.REJECTED -> throw IllegalStateException("LOGIN_REJECTED")
        }
    }

    /** 승인 상태 폴링 (프론트에서 PENDING 상태일 때 주기적으로 호출) */
    fun checkStatus(kakaoId: String): LoginResponse {
        val approval = approvalRepository.findById(kakaoId)
            .orElseThrow { NotFoundException("Approval not found: $kakaoId") }
        val user = kakaoUserRepository.findById(kakaoId)
            .orElseThrow { NotFoundException("User not found: $kakaoId") }

        return when (approval.status) {
            ApprovalStatus.APPROVED -> LoginResponse(
                status = "APPROVED",
                kakaoId = user.kakaoId,
                accessToken = jwtService.generate(user.kakaoId, user.memberId),
                memberId = user.memberId,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl
            )
            ApprovalStatus.PENDING -> LoginResponse(
                status = "PENDING",
                kakaoId = user.kakaoId,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl
            )
            ApprovalStatus.REJECTED -> LoginResponse(
                status = "REJECTED",
                kakaoId = user.kakaoId,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl
            )
        }
    }

    fun approve(kakaoId: String): ApprovalItemResponse {
        val approval = approvalRepository.findById(kakaoId)
            .orElseThrow { NotFoundException("Approval not found: $kakaoId") }
        approval.status = ApprovalStatus.APPROVED
        approval.processedAt = Instant.now().toString()
        return approvalRepository.save(approval).toResponse()
    }

    fun reject(kakaoId: String): ApprovalItemResponse {
        val approval = approvalRepository.findById(kakaoId)
            .orElseThrow { NotFoundException("Approval not found: $kakaoId") }
        approval.status = ApprovalStatus.REJECTED
        approval.processedAt = Instant.now().toString()
        return approvalRepository.save(approval).toResponse()
    }

    fun getPendingApprovals(): List<ApprovalItemResponse> =
        approvalRepository.findByStatusOrderByRequestedAtDesc(ApprovalStatus.PENDING)
            .map { it.toResponse() }

    fun getAllApprovals(): List<ApprovalItemResponse> =
        approvalRepository.findAll().sortedByDescending { it.requestedAt }.map { it.toResponse() }

    fun linkMember(kakaoId: String, memberId: String): LoginResponse {
        val user = kakaoUserRepository.findById(kakaoId)
            .orElseThrow { NotFoundException("User not found: $kakaoId") }
        user.memberId = memberId
        kakaoUserRepository.save(user)
        return LoginResponse(
            status = "APPROVED",
            kakaoId = user.kakaoId,
            accessToken = jwtService.generate(user.kakaoId, memberId),
            memberId = memberId,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl
        )
    }

    private fun fetchKakaoToken(code: String, redirectUri: String): String {
        val form = listOf(
            "grant_type=authorization_code",
            "client_id=${URLEncoder.encode(restApiKey, StandardCharsets.UTF_8)}",
            "redirect_uri=${URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)}",
            "code=${URLEncoder.encode(code, StandardCharsets.UTF_8)}"
        ).joinToString("&")

        val request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUri))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        val node = objectMapper.readTree(response.body())
        return node.path("access_token").asText()
            .also { if (it.isBlank()) throw IllegalStateException("Kakao token 발급 실패: ${response.body()}") }
    }

    private fun fetchKakaoUserInfo(kakaoToken: String): KakaoUserInfo {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(userInfoUri))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $kakaoToken")
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        val node = objectMapper.readTree(response.body())
        val profile = node.path("kakao_account").path("profile")
        return KakaoUserInfo(
            id = node.path("id").asText(),
            nickname = profile.path("nickname").asText(""),
            profileImageUrl = profile.path("profile_image_url").asText().ifBlank { null }
        )
    }

    private data class KakaoUserInfo(val id: String, val nickname: String, val profileImageUrl: String?)
}
