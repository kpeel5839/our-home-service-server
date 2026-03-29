package com.ourhome.server.domain.stock

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stocks")
class StockController(private val stockService: StockService) {

    @GetMapping
    fun getAll(): ResponseEntity<List<StockPriceResponse>> =
        ResponseEntity.ok(stockService.getAll())

    @PostMapping
    fun add(@RequestBody req: AddStockRequest): ResponseEntity<TrackedStock> =
        ResponseEntity.ok(stockService.add(req))

    @DeleteMapping("/{symbol}")
    fun delete(@PathVariable symbol: String): ResponseEntity<Void> {
        stockService.delete(symbol)
        return ResponseEntity.noContent().build()
    }
}
