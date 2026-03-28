package com.ourhome.server.domain.notification

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class DiscordNotificationService(
    @Value("\${discord.webhook-url}") private val webhookUrl: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun sendMessage(content: String) {
        if (webhookUrl.isBlank()) {
            log.warn("[Discord] webhook-url이 설정되지 않아 메시지를 전송하지 않습니다.")
            return
        }
        try {
            // Discord 메시지는 2000자 제한 — 초과 시 분할 전송
            content.chunked(1990).forEach { chunk ->
                val body = """{"content":${escapeJson(chunk)}}"""
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() in 200..204) {
                    log.info("[Discord] 메시지 전송 성공")
                } else {
                    log.error("[Discord] 전송 실패: status=${response.statusCode()} body=${response.body()}")
                }
            }
        } catch (e: Exception) {
            log.error("[Discord] 전송 중 오류: ${e.message}", e)
        }
    }

    private fun escapeJson(s: String): String =
        "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}
