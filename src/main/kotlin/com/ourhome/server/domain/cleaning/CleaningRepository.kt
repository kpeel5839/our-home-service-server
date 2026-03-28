package com.ourhome.server.domain.cleaning

import org.springframework.data.jpa.repository.JpaRepository

interface CleaningTaskRepository : JpaRepository<CleaningTask, String>

interface CleaningAssignmentRepository : JpaRepository<CleaningAssignment, String> {
    fun findByScheduledDate(scheduledDate: String): List<CleaningAssignment>
}
