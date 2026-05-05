package com.ourhome.server.domain.notification

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class DiscordNotificationService(private val props: DiscordProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    /** 기본 웹훅으로 전송 (기존 동작 유지) */
    @Async
    fun sendMessage(content: String) {
        send(props.webhookUrl, content)
    }

    /**
     * 특정 멤버 역할(SON/DAUGHTER/FATHER/MOTHER)의 웹훅으로 전송.
     * 해당 역할 웹훅이 없으면 기본 웹훅으로 폴백.
     */
    @Async
    fun sendToRole(role: String, content: String) {
        val url = props.memberWebhooks[role.uppercase()]
            ?: props.webhookUrl.takeIf { it.isNotBlank() }
            ?: return Unit.also { log.warn("[Discord] 웹훅 없음 - role=$role") }
        send(url, content)
    }

    /** 가족 멤버 웹훅으로만 전송 (주차·주유·외박 등 생활 알림용) */
    @Async
    fun sendToAllMembers(content: String) {
        val urls = props.memberWebhooks.values.toSet()
            .ifEmpty { setOf(props.webhookUrl) }
            .filter { it.isNotBlank() }
        urls.forEach { url -> send(url, content) }
    }

    /** 가족 멤버 웹훅 + market 웹훅으로 전송 (차량 예약·주식 알림용) */
    @Async
    fun sendToAll(content: String) {
        val urls = (props.memberWebhooks.values + listOfNotNull(props.marketWebhookUrl.takeIf { it.isNotBlank() }))
            .toSet()
            .ifEmpty { setOf(props.webhookUrl) }
            .filter { it.isNotBlank() }
        urls.forEach { url -> send(url, content) }
    }

    private fun send(url: String, content: String) {
        if (url.isBlank()) {
            log.warn("[Discord] webhook-url이 설정되지 않아 전송하지 않습니다.")
            return
        }
        content.chunked(1990).forEach { chunk ->
            val body = """{"content":${escapeJson(chunk)}}"""
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept { response ->
                    if (response.statusCode() in 200..204) {
                        log.info("[Discord] 전송 성공 → ${url.takeLast(20)}")
                    } else {
                        log.error("[Discord] 전송 실패: status=${response.statusCode()} body=${response.body()}")
                    }
                }
                .exceptionally { e ->
                    log.error("[Discord] 전송 중 오류: ${e.message}", e)
                    null
                }
        }
    }

    private fun escapeJson(s: String): String =
        "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}
