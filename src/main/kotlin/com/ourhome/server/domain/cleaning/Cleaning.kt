package com.ourhome.server.domain.cleaning

import jakarta.persistence.*
import java.util.UUID

enum class CleaningFrequency { DAILY, WEEKLY, BIWEEKLY, MONTHLY }

@Entity
@Table(name = "cleaning_tasks")
class CleaningTask(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var frequency: CleaningFrequency,

    var dayOfWeek: Int? = null,

    @Column(nullable = false)
    var assignedMemberId: String
)

@Entity
@Table(name = "cleaning_assignments")
class CleaningAssignment(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val taskId: String,

    @Column(nullable = false)
    val memberId: String,

    @Column(nullable = false)
    val scheduledDate: String,

    var isCompleted: Boolean = false
)

data class CleaningTaskResponse(
    val id: String, val name: String, val frequency: String,
    val dayOfWeek: Int?, val assignedMemberId: String
)

data class CleaningAssignmentResponse(
    val id: String, val taskId: String, val memberId: String,
    val scheduledDate: String, val isCompleted: Boolean
)

data class CreateCleaningTaskRequest(
    val name: String, val frequency: CleaningFrequency,
    val dayOfWeek: Int? = null, val assignedMemberId: String
)

data class UpdateCleaningTaskRequest(
    val name: String? = null, val frequency: CleaningFrequency? = null,
    val dayOfWeek: Int? = null, val assignedMemberId: String? = null
)

fun CleaningTask.toResponse() = CleaningTaskResponse(
    id = id, name = name, frequency = frequency.name,
    dayOfWeek = dayOfWeek, assignedMemberId = assignedMemberId
)

fun CleaningAssignment.toResponse() = CleaningAssignmentResponse(
    id = id, taskId = taskId, memberId = memberId,
    scheduledDate = scheduledDate, isCompleted = isCompleted
)
