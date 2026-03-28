package com.ourhome.server.domain.notification

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 매일 오전 8시 (KST = UTC+9) 에 금융 시장 데이터를 수집해서 Discord로 전송합니다.
 * cron: "0 0 8 * * *"  — 초 분 시 일 월 요일, timezone = Asia/Seoul
 */
@Component
class MarketNotificationScheduler(
    private val marketDataService: MarketDataService,
    private val discordNotificationService: DiscordNotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    fun sendDailyMarketReport() {
        log.info("[MarketNotification] 일일 시장 알림 전송 시작")
        try {
            val data = marketDataService.fetchAllMarketData()
            val message = buildMessage(data)
            discordNotificationService.sendMessage(message)
        } catch (e: Exception) {
            log.error("[MarketNotification] 알림 전송 실패: ${e.message}", e)
        }
    }

    private fun buildMessage(data: MarketDataService.MarketData): String {
        val today = LocalDate.now()
        val sb = StringBuilder()

        sb.appendLine("📊 [$today] 오늘의 금융 시장")
        sb.appendLine("─────────────────────")

        sb.appendLine("\n📈 주가 지수")
        data.stockIndices.forEach { (name, idx) ->
            val arrow = if (idx.change.startsWith("-")) "▼" else "▲"
            sb.appendLine("  $name  ${idx.value}  $arrow ${idx.change} (${idx.changeRate})")
        }

        sb.appendLine("\n💴 환율")
        data.exchangeRates.forEach { (name, fx) ->
            val arrow = if (fx.change.startsWith("-")) "▼" else "▲"
            sb.appendLine("  $name  ${fx.value}  $arrow ${fx.change}")
        }

        sb.appendLine("\n🏦 국채 금리")
        data.bondRates.forEach { (name, bond) ->
            sb.appendLine("  $name  ${bond.value}")
        }

        return sb.toString().trimEnd()
    }
}
