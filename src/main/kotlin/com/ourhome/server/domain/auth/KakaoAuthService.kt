package com.ourhome.server.domain.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
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
    @Value("\${kakao.client-secret:}") private val clientSecret: String,
    @Value("\${kakao.redirect-uri}") private val redirectUri: String,
    @Value("\${kakao.token-uri}") private val tokenUri: String,
    @Value("\${kakao.user-info-uri}") private val userInfoUri: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    companion object {
        private val NAME_TO_MEMBER = mapOf(
            "김기필" to "m1",  // 아빠
            "윤재희" to "m2",  // 엄마
            "김재연" to "m3",  // 아들
            "김정은" to "m4"   // 딸
        )
    }

    fun login(code: String): AuthResponse {
        val kakaoToken = fetchKakaoToken(code)
        val userInfo = fetchKakaoUserInfo(kakaoToken)

        val memberId = NAME_TO_MEMBER.entries
            .firstOrNull { (name, _) -> userInfo.nickname.contains(name) }
            ?.value ?: throw IllegalStateException("LOGIN_NOT_ALLOWED")

        val user = findOrCreateUser(userInfo, memberId)

        return AuthResponse(
            accessToken = jwtService.generate(user.kakaoId, memberId),
            memberId = memberId,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl
        )
    }

    /**
     * 유저 조회 or 생성.
     * saveAndFlush 로 즉시 flush 하여 중복 키 예외를 이 시점에 잡고,
     * React StrictMode 등 동시 요청으로 이미 저장된 경우 findById 로 fallback.
     */
    private fun findOrCreateUser(userInfo: KakaoUserInfo, memberId: String): KakaoUser {
        val existing = kakaoUserRepository.findByKakaoId(userInfo.id)
        if (existing != null) {
            var dirty = false
            if (existing.nickname != userInfo.nickname)               { existing.nickname = userInfo.nickname; dirty = true }
            if (existing.profileImageUrl != userInfo.profileImageUrl) { existing.profileImageUrl = userInfo.profileImageUrl; dirty = true }
            if (existing.memberId != memberId)                        { existing.memberId = memberId; dirty = true }
            if (dirty) kakaoUserRepository.save(existing)
            return existing
        }

        return try {
            kakaoUserRepository.saveAndFlush(
                KakaoUser(kakaoId = userInfo.id, nickname = userInfo.nickname,
                    profileImageUrl = userInfo.profileImageUrl, memberId = memberId)
            )
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청(React StrictMode 등)으로 이미 다른 요청이 먼저 저장함 → 읽어서 반환
            log.warn("Concurrent login for kakaoId=${userInfo.id}, falling back to findByKakaoId")
            kakaoUserRepository.findByKakaoId(userInfo.id) ?: throw e
        }
    }

    private fun fetchKakaoToken(code: String): String {
        val params = mutableListOf(
            "grant_type=authorization_code",
            "client_id=${URLEncoder.encode(restApiKey, StandardCharsets.UTF_8)}",
            "redirect_uri=${URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)}",
            "code=${URLEncoder.encode(code, StandardCharsets.UTF_8)}"
        )
        if (clientSecret.isNotBlank()) {
            params.add("client_secret=${URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)}")
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUri))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(params.joinToString("&")))
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
