package com.ourhome.server.domain.notification

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

/**
 * 카카오톡 알림톡 / 메시지 API를 통해 메시지를 전송합니다.
 *
 * 설정 (application.yml 또는 환경변수):
 *   kakao.access-token    — 카카오 REST API 액세스 토큰
 *   kakao.template-id     — 알림톡 템플릿 ID (알림톡 사용 시)
 *   kakao.sender-key      — 카카오 비즈 플러스친구 sender key (알림톡 사용 시)
 *   kakao.receiver-uuids  — 수신자 UUID 목록 (쉼표 구분), 나에게 보내기 토큰 등
 *
 * 나에게 보내기 방식: 개인 액세스 토큰 하나로 간단히 동작합니다.
 */
@Service
class KakaoNotificationService(
    @Value("\${kakao.access-token:}") private val accessToken: String,
    @Value("\${kakao.use-me-api:true}") private val useMeApi: Boolean
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    /**
     * 카카오톡 나에게 보내기 API로 메시지 전송
     * 액세스 토큰 발급: https://developers.kakao.com/console/app → 카카오 로그인 → 토큰 발급
     */
    fun sendMessage(text: String) {
        if (accessToken.isBlank()) {
            log.warn("[KakaoNotification] access-token이 설정되지 않아 메시지를 전송하지 않습니다. text=\n$text")
            return
        }

        try {
            val templateObject = buildTextTemplate(text)
            val form = "template_object=${URLEncoder.encode(templateObject, StandardCharsets.UTF_8)}"
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://kapi.kakao.com/v2/api/talk/memo/default/send"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                log.info("[KakaoNotification] 메시지 전송 성공")
            } else {
                log.error("[KakaoNotification] 전송 실패: status=${response.statusCode()} body=${response.body()}")
            }
        } catch (e: Exception) {
            log.error("[KakaoNotification] 전송 중 오류: ${e.message}", e)
        }
    }

    private fun buildTextTemplate(text: String): String =
        """{"object_type":"text","text":${escapeJson(text)},"link":{"web_url":"","mobile_web_url":""}}"""

    private fun escapeJson(s: String): String =
        "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}
