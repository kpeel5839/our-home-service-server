package com.ourhome.server.domain.notification

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Naver Finance 공개 API를 통해 시장 데이터를 수집합니다.
 * API Key 불필요, 별도 인프라 없이 동작합니다.
 */
@Service
class MarketDataService(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    data class MarketData(
        val stockIndices: Map<String, IndexData>,
        val exchangeRates: Map<String, ExchangeData>,
        val bondRates: Map<String, BondData>
    )

    data class IndexData(val name: String, val value: String, val change: String, val changeRate: String)
    data class ExchangeData(val name: String, val value: String, val change: String)
    data class BondData(val name: String, val value: String)

    fun fetchAllMarketData(): MarketData {
        val indices = fetchStockIndices()
        val fx = fetchExchangeRates()
        val bonds = fetchBondRates()
        return MarketData(indices, fx, bonds)
    }

    private fun fetchStockIndices(): Map<String, IndexData> {
        // Naver Finance 시세 API
        val symbols = mapOf(
            "KOSPI" to "KOSPI",
            "NASDAQ" to "NAS",
            "S&P500" to "SPI@SPX"
        )
        val result = mutableMapOf<String, IndexData>()
        symbols.forEach { (displayName, code) ->
            try {
                val url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_INDEX:$code"
                val body = get(url, mapOf("Referer" to "https://finance.naver.com"))
                val node = objectMapper.readTree(body)
                val item = node.path("result").path("areas").firstOrNull()
                    ?.path("datas")?.firstOrNull()
                if (item != null && !item.isMissingNode) {
                    result[displayName] = IndexData(
                        name = displayName,
                        value = formatNumber(item.path("nv").asText("")),
                        change = item.path("cv").asText("0"),
                        changeRate = item.path("cr").asText("0") + "%"
                    )
                }
            } catch (e: Exception) {
                log.warn("Failed to fetch $displayName: ${e.message}")
                result[displayName] = IndexData(displayName, "N/A", "N/A", "N/A")
            }
        }
        return result
    }

    private fun fetchExchangeRates(): Map<String, ExchangeData> {
        // Naver Finance 환율 API
        val codes = mapOf(
            "USD/KRW" to "FX_USDKRW",
            "EUR/KRW" to "FX_EURKRW",
            "JPY/KRW" to "FX_JPYKRW",
            "CNY/KRW" to "FX_CNYKRW",
            "DXY" to "FX_USDIDX"
        )
        val result = mutableMapOf<String, ExchangeData>()
        codes.forEach { (displayName, code) ->
            try {
                val url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_INDEX:$code"
                val body = get(url, mapOf("Referer" to "https://finance.naver.com"))
                val node = objectMapper.readTree(body)
                val item = node.path("result").path("areas").firstOrNull()
                    ?.path("datas")?.firstOrNull()
                if (item != null && !item.isMissingNode) {
                    result[displayName] = ExchangeData(
                        name = displayName,
                        value = item.path("nv").asText("N/A"),
                        change = item.path("cv").asText("0")
                    )
                }
            } catch (e: Exception) {
                log.warn("Failed to fetch $displayName: ${e.message}")
                result[displayName] = ExchangeData(displayName, "N/A", "N/A")
            }
        }
        return result
    }

    private fun fetchBondRates(): Map<String, BondData> {
        // 국채 금리 — Naver Finance 채권 시세
        val bonds = mapOf(
            "한국 국채 10년" to "MMFS@KR10YT=RR",
            "한국 국채 3년" to "MMFS@KR3YT=RR",
            "미국 국채 10년" to "MMFS@US10YT=RR",
            "미국 국채 2년" to "MMFS@US2YT=RR"
        )
        val result = mutableMapOf<String, BondData>()
        bonds.forEach { (displayName, code) ->
            try {
                val url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:$code"
                val body = get(url, mapOf("Referer" to "https://finance.naver.com"))
                val node = objectMapper.readTree(body)
                val item = node.path("result").path("areas").firstOrNull()
                    ?.path("datas")?.firstOrNull()
                val value = if (item != null && !item.isMissingNode) item.path("nv").asText("N/A") else "N/A"
                result[displayName] = BondData(displayName, "$value%")
            } catch (e: Exception) {
                log.warn("Failed to fetch $displayName: ${e.message}")
                result[displayName] = BondData(displayName, "N/A")
            }
        }
        return result
    }

    private fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
        headers.forEach { (k, v) -> builder.header(k, v) }
        builder.header("User-Agent", "Mozilla/5.0 (compatible; OurHomeService/1.0)")
        val response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun formatNumber(value: String): String = try {
        val d = value.toDouble()
        String.format("%,.2f", d)
    } catch (e: NumberFormatException) {
        value
    }
}
