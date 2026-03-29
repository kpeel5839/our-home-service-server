package com.ourhome.server.domain.notification

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/market")
class MarketController(private val marketDataService: MarketDataService) {

    @GetMapping
    fun getMarketData(): ResponseEntity<MarketDataService.MarketData> =
        ResponseEntity.ok(marketDataService.fetchAllMarketData())
}
