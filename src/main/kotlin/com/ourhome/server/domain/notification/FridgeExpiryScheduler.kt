package com.ourhome.server.domain.notification

import com.ourhome.server.domain.fridge.FridgeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class FridgeExpiryScheduler(
    private val fridgeRepository: FridgeRepository,
    private val discordNotificationService: DiscordNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 매일 오전 9시 (KST) — 유통기한 D-3 이하 품목 알림 */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    fun sendExpiryReminders() {
        val today = LocalDate.now()
        val warnDate = today.plusDays(3).toString()

        val items = fridgeRepository.findByIsConsumedFalse()
            .filter { it.expirationDate <= warnDate }
            .sortedBy { it.expirationDate }

        if (items.isEmpty()) return

        log.info("[FridgeExpiry] 유통기한 임박 ${items.size}건")

        val lines = items.joinToString("\n") { item ->
            val daysLeft = today.until(LocalDate.parse(item.expirationDate)).days
            val label = when {
                daysLeft < 0 -> "⛔ 만료됨"
                daysLeft == 0 -> "🔴 오늘 만료"
                else -> "🟡 D-${daysLeft}"
            }
            "$label ${item.name} (${item.quantity}${item.unit})"
        }

        discordNotificationService.sendToAllMembers("⚠️ 유통기한 임박 식품이 있어요!\n$lines")
    }
}
