package com.ourhome.server.domain.attendance

import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceRepository : JpaRepository<AttendanceStatus, String> {
    fun findByDate(date: String): List<AttendanceStatus>
}
