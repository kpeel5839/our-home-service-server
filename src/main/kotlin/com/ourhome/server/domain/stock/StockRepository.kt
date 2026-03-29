package com.ourhome.server.domain.stock

import org.springframework.data.jpa.repository.JpaRepository

interface StockRepository : JpaRepository<TrackedStock, String>
