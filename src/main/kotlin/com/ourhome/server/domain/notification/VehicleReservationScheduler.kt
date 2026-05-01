package com.ourhome.server.domain.notification

import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.vehicle.VehicleReservationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class VehicleReservationScheduler(
    private val reservationRepository: VehicleReservationRepository,
    private val memberRepository: MemberRepository,
    private val discordNotificationService: DiscordNotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 매일 오후 8시 (KST) — 내일 예약 알림 */
    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    fun sendDayBeforeReminders() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val reservations = reservationRepository.findByStartTimeStartingWith(tomorrow)
        if (reservations.isEmpty()) return
        log.info("[VehicleScheduler] 내일 예약 알림 ${reservations.size}건")
        reservations.forEach { r ->
            val name = memberName(r.memberId)
            val timeStr = "${formatTime(r.startTime)}~${formatTime(r.endTime)}"
            val purposePart = if (!r.purpose.isNullOrBlank()) "\n목적: ${r.purpose}" else ""
            discordNotificationService.sendToAll("🔔 내일 ${name}님의 차량 예약이 있어요\n$timeStr$purposePart")
        }
    }

    /** 매일 오전 8시 (KST) — 오늘 예약 알림 */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    fun sendDayOfReminders() {
        val today = LocalDate.now().toString()
        val reservations = reservationRepository.findByStartTimeStartingWith(today)
        if (reservations.isEmpty()) return
        log.info("[VehicleScheduler] 오늘 예약 알림 ${reservations.size}건")
        reservations.forEach { r ->
            val name = memberName(r.memberId)
            val timeStr = "${formatTime(r.startTime)}~${formatTime(r.endTime)}"
            val purposePart = if (!r.purpose.isNullOrBlank()) "\n목적: ${r.purpose}" else ""
            discordNotificationService.sendToAll("🚗 오늘 ${name}님의 차량 예약일이에요!\n$timeStr$purposePart")
        }
    }

    private fun memberName(memberId: String): String =
        memberRepository.findById(memberId).map { it.name }.orElse("알 수 없음")

    private fun formatTime(isoTime: String): String =
        if (isoTime.length >= 16) isoTime.substring(11, 16) else isoTime
}
