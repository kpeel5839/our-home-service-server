package com.ourhome.server.domain.attendance

import com.ourhome.server.config.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/attendance")
class AttendanceController(private val attendanceRepository: AttendanceRepository) {

    @GetMapping
    fun getAttendance(@RequestParam date: String): ResponseEntity<List<AttendanceResponse>> =
        ResponseEntity.ok(attendanceRepository.findByDate(date).map { it.toResponse() })

    @PostMapping
    fun createAttendance(@RequestBody request: CreateAttendanceRequest): ResponseEntity<AttendanceResponse> {
        val attendance = AttendanceStatus(
            date = request.date,
            memberId = request.memberId,
            status = request.status,
            expectedReturnTime = request.expectedReturnTime,
            memo = request.memo
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceRepository.save(attendance).toResponse())
    }

    @PatchMapping("/{id}")
    fun updateAttendance(
        @PathVariable id: String,
        @RequestBody request: UpdateAttendanceRequest
    ): ResponseEntity<AttendanceResponse> {
        val attendance = attendanceRepository.findById(id).orElseThrow { NotFoundException("Attendance not found: $id") }
        request.status?.let { attendance.status = it }
        request.expectedReturnTime?.let { attendance.expectedReturnTime = it }
        request.memo?.let { attendance.memo = it }
        return ResponseEntity.ok(attendanceRepository.save(attendance).toResponse())
    }
}
