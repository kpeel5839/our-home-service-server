package com.ourhome.server.domain.attendance

import com.ourhome.server.config.ForbiddenException
import com.ourhome.server.config.NotFoundException
import com.ourhome.server.config.SecurityUtils
import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.notification.DiscordNotificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/attendance")
class AttendanceController(
    private val attendanceRepository: AttendanceRepository,
    private val memberRepository: MemberRepository,
    private val discordNotificationService: DiscordNotificationService
) {

    private fun memberName(memberId: String): String =
        memberRepository.findById(memberId).map { it.name }.orElse("알 수 없음")

    @GetMapping
    fun getAttendance(@RequestParam date: String): ResponseEntity<List<AttendanceResponse>> =
        ResponseEntity.ok(attendanceRepository.findByDate(date).map { it.toResponse() })

    @PostMapping
    fun createAttendance(@RequestBody request: CreateAttendanceRequest): ResponseEntity<AttendanceResponse> {
        val memberId = SecurityUtils.currentMemberId()
        val attendance = AttendanceStatus(
            date = request.date,
            memberId = memberId,
            status = request.status,
            expectedReturnTime = request.expectedReturnTime,
            memo = request.memo
        )
        val saved = attendanceRepository.save(attendance)
        if (request.status == AttendanceStatusType.OUT) {
            val name = memberName(memberId)
            val memoPart = if (!request.memo.isNullOrBlank()) "\n메모: ${request.memo}" else ""
            discordNotificationService.sendToAllMembers("🌙 ${name}님이 오늘 외박이에요$memoPart")
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @PatchMapping("/{id}")
    fun updateAttendance(
        @PathVariable id: String,
        @RequestBody request: UpdateAttendanceRequest
    ): ResponseEntity<AttendanceResponse> {
        val memberId = SecurityUtils.currentMemberId()
        val attendance = attendanceRepository.findById(id).orElseThrow { NotFoundException("Attendance not found: $id") }
        if (attendance.memberId != memberId) throw ForbiddenException("본인의 출석 정보만 수정할 수 있습니다")
        val previousStatus = attendance.status
        request.status?.let { attendance.status = it }
        if (request.clearReturnTime == true) attendance.expectedReturnTime = null
        else request.expectedReturnTime?.let { attendance.expectedReturnTime = it }
        request.memo?.let { attendance.memo = it }
        val saved = attendanceRepository.save(attendance)
        val newStatus = saved.status
        if (previousStatus != AttendanceStatusType.OUT && newStatus == AttendanceStatusType.OUT) {
            val name = memberName(saved.memberId)
            val memoPart = if (!saved.memo.isNullOrBlank()) "\n메모: ${saved.memo}" else ""
            discordNotificationService.sendToAllMembers("🌙 ${name}님이 오늘 외박이에요$memoPart")
        }
        return ResponseEntity.ok(saved.toResponse())
    }
}
