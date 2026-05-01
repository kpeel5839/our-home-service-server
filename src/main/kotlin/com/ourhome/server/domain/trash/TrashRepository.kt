package com.ourhome.server.domain.trash

import org.springframework.data.jpa.repository.JpaRepository

interface TrashScheduleRepository : JpaRepository<TrashSchedule, String> {
    fun findByDate(date: String): List<TrashSchedule>
    fun findByDateBetweenOrderByDate(from: String, to: String): List<TrashSchedule>
}

interface ApartmentTrashRuleRepository : JpaRepository<ApartmentTrashRule, String>
