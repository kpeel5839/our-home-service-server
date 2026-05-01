package com.ourhome.server.domain.notification

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * discord.webhook-url        — 기본(폴백) 웹훅
 * discord.member-webhooks    — 멤버 역할별 웹훅 (SON, DAUGHTER, FATHER, MOTHER)
 */
@Component
@ConfigurationProperties(prefix = "discord")
class DiscordProperties {
    var webhookUrl: String = ""
    var marketWebhookUrl: String = ""
    var memberWebhooks: Map<String, String> = emptyMap()
}
