package com.ourhome.server.domain.notification

import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.vehicle.VehicleReservationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class VehicleReservationEndScheduler(
    private val reservationRepository: VehicleReservationRepository,
    private val memberRepository: MemberRepository,
    private val discordNotificationService: DiscordNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 매 15분마다 — 방금 끝난 예약 종료 알림 */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    fun notifyReservationEnd() {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val windowEnd = now.toString().substring(0, 16)          // "YYYY-MM-DDTHH:MM"
        val windowStart = now.minusMinutes(15).toString().substring(0, 16)

        val ended = reservationRepository.findByEndTimeBetween(
            "${windowStart}:00", "${windowEnd}:59"
        ).filter { it.approved }

        log.info("[VehicleReservationEnd] 스케줄러 실행 — window=$windowStart~$windowEnd, 건수=${ended.size}")

        ended.forEach { reservation ->
            val member = memberRepository.findById(reservation.memberId).orElse(null)
            val name = member?.name ?: "알 수 없음"
            val role = member?.role?.name ?: "OTHER"
            discordNotificationService.sendToRole(
                role,
                "🚗 ${name}님의 차량 예약이 끝났어요!\n주차 위치와 주유량을 앱에서 기록해주세요."
            )
        }
    }
}
