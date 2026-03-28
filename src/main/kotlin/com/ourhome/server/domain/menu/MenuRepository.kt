package com.ourhome.server.domain.menu

import org.springframework.data.jpa.repository.JpaRepository

interface MenuRepository : JpaRepository<DailyMenu, String> {
    fun findByDate(date: String): List<DailyMenu>
}
