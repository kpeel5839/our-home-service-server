package com.ourhome.server.domain.attendance

import jakarta.persistence.*
import java.util.UUID

enum class AttendanceStatusType { HOME, OUT, UNKNOWN }

@Entity
@Table(name = "attendance_statuses")
class AttendanceStatus(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val date: String,

    @Column(nullable = false)
    val memberId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AttendanceStatusType,

    var expectedReturnTime: String? = null,
    var memo: String? = null
)

data class AttendanceResponse(
    val id: String, val date: String, val memberId: String,
    val status: String, val expectedReturnTime: String?, val memo: String?
)

data class CreateAttendanceRequest(
    val date: String, val memberId: String, val status: AttendanceStatusType,
    val expectedReturnTime: String? = null, val memo: String? = null
)

data class UpdateAttendanceRequest(
    val status: AttendanceStatusType? = null,
    val expectedReturnTime: String? = null,
    val memo: String? = null
)

fun AttendanceStatus.toResponse() = AttendanceResponse(
    id = id, date = date, memberId = memberId, status = status.name,
    expectedReturnTime = expectedReturnTime, memo = memo
)
