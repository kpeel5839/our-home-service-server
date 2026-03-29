package com.ourhome.server.domain.stock

import jakarta.persistence.*

@Entity
@Table(name = "tracked_stocks")
class TrackedStock(
    @Id
    val symbol: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val displayOrder: Int = 0
)

data class StockPriceResponse(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val currency: String
)

data class AddStockRequest(
    val symbol: String,
    val name: String,
    val displayOrder: Int = 0
)
