package com.ourhome.server.domain.notification

import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.trash.TrashScheduleRepository
import com.ourhome.server.domain.trash.TrashType
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class TrashNotificationScheduler(
    private val trashScheduleRepository: TrashScheduleRepository,
    private val memberRepository: MemberRepository,
    private val discordNotificationService: DiscordNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val trashLabels = mapOf(
        TrashType.GENERAL to "일반쓰레기",
        TrashType.RECYCLE to "재활용",
        TrashType.FOOD to "음식물",
        TrashType.LARGE to "대형폐기물"
    )

    /** 매일 오전 6시 (KST) — 오늘 쓰레기 배출 담당자 알림 */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    fun sendTrashReminders() {
        val today = LocalDate.now().toString()
        val schedules = trashScheduleRepository.findByDate(today)
        log.info("[TrashNotification] 스케줄러 실행 — today=$today, 건수=${schedules.size}")
        if (schedules.isEmpty()) return
        schedules.forEach { schedule ->
            val member = memberRepository.findById(schedule.memberId).orElse(null)
            val name = member?.name ?: "알 수 없음"
            val role = member?.role?.name ?: "OTHER"
            val types = schedule.trashTypes.joinToString(", ") { trashLabels[it] ?: it.name }
            discordNotificationService.sendToRole(role, "🗑️ ${name}님, 오늘 쓰레기 버리는 날이에요!\n$types")
        }
    }
}
