package com.ourhome.server.domain.auth

import com.fasterxml.jackson.databind.ObjectMapper
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

@Service
class KakaoAuthService(
    private val kakaoUserRepository: KakaoUserRepository,
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper,
    @Value("\${kakao.rest-api-key}") private val restApiKey: String,
    @Value("\${kakao.token-uri}") private val tokenUri: String,
    @Value("\${kakao.user-info-uri}") private val userInfoUri: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    fun login(code: String, redirectUri: String): AuthResponse {
        val kakaoToken = fetchKakaoToken(code, redirectUri)
        val userInfo = fetchKakaoUserInfo(kakaoToken)

        val user = kakaoUserRepository.findById(userInfo.id).orElseGet {
            kakaoUserRepository.save(KakaoUser(
                kakaoId = userInfo.id,
                nickname = userInfo.nickname,
                profileImageUrl = userInfo.profileImageUrl
            ))
        }.also { existing ->
            // 닉네임/프로필 변경 시 업데이트
            if (existing.nickname != userInfo.nickname || existing.profileImageUrl != userInfo.profileImageUrl) {
                existing.nickname = userInfo.nickname
                existing.profileImageUrl = userInfo.profileImageUrl
                kakaoUserRepository.save(existing)
            }
        }

        return AuthResponse(
            accessToken = jwtService.generate(user.kakaoId, user.memberId),
            memberId = user.memberId,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl
        )
    }

    fun linkMember(kakaoId: String, memberId: String): AuthResponse {
        val user = kakaoUserRepository.findById(kakaoId)
            .orElseThrow { IllegalArgumentException("User not found: $kakaoId") }
        user.memberId = memberId
        kakaoUserRepository.save(user)
        return AuthResponse(
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
