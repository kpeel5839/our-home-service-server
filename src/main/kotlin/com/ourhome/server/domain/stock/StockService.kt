package com.ourhome.server.domain.stock

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class StockService(
    private val stockRepository: StockRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    fun getAll(): List<StockPriceResponse> =
        stockRepository.findAll()
            .sortedBy { it.displayOrder }
            .mapNotNull { stock ->
                try {
                    fetchPrice(stock)
                } catch (e: Exception) {
                    log.warn("주가 조회 실패 - symbol=${stock.symbol}: ${e.message}")
                    null
                }
            }

    fun add(req: AddStockRequest): TrackedStock {
        val stock = TrackedStock(
            symbol = req.symbol.uppercase().trim(),
            name = req.name.trim(),
            displayOrder = req.displayOrder
        )
        return stockRepository.save(stock)
    }

    fun delete(symbol: String) = stockRepository.deleteById(symbol.uppercase())

    private fun fetchPrice(stock: TrackedStock): StockPriceResponse {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/${stock.symbol}?interval=1d&range=1d"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(8))
            .header("User-Agent", "Mozilla/5.0")
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        val root = objectMapper.readTree(response.body())
        val meta = root.path("chart").path("result").get(0).path("meta")

        val price = meta.path("regularMarketPrice").asDouble()
        val change = meta.path("regularMarketChange").asDouble()
        val changePercent = meta.path("regularMarketChangePercent").asDouble()
        val currency = meta.path("currency").asText("USD")

        return StockPriceResponse(
            symbol = stock.symbol,
            name = stock.name,
            price = price,
            change = change,
            changePercent = changePercent,
            currency = currency
        )
    }
}
