package com.ourhome.server.domain.notification

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * 시장 데이터 수집 서비스
 * - 주가지수 / 환율: Yahoo Finance v8 Chart API
 * - 미국 국채(10년·2년) / 한국 국채(10년): CNBC Quote API (단일 요청)
 * - 한국 국채(3년): Naver Finance interestDailyQuote HTML 파싱
 * - 모든 HTTP 요청은 코루틴으로 병렬 처리
 */
@Service
class MarketDataService(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    data class MarketData(
        val stockIndices: Map<String, IndexData>,
        val exchangeRates: Map<String, ExchangeData>,
        val bondRates: Map<String, BondData>
    )

    data class IndexData(val name: String, val value: String, val change: String, val changeRate: String)
    data class ExchangeData(val name: String, val value: String, val change: String)
    data class BondData(val name: String, val value: String)

    fun fetchAllMarketData(): MarketData = runBlocking {
        // 모든 요청 병렬 실행
        val kospiD = async(Dispatchers.IO) { yahooIndex("KOSPI", "^KS11") }
        val nasdaqD = async(Dispatchers.IO) { yahooIndex("NASDAQ", "^IXIC") }
        val sp500D = async(Dispatchers.IO) { yahooIndex("S&P500", "^GSPC") }

        val usdD = async(Dispatchers.IO) { yahooFx("USD/KRW", "USDKRW=X") }
        val eurD = async(Dispatchers.IO) { yahooFx("EUR/KRW", "EURKRW=X") }
        val jpyD = async(Dispatchers.IO) { yahooFx("JPY/KRW", "JPYKRW=X") }
        val dxyD = async(Dispatchers.IO) { yahooFx("DXY", "DX-Y.NYB") }

        // CNBC API: US 10Y/2Y + 한국 10Y 한 번에 (3개 심볼 단일 요청)
        val cnbcD = async(Dispatchers.IO) { fetchCnbcBonds() }
        // 한국 3년 국채: Naver Finance
        val kr3yD = async(Dispatchers.IO) { naverBond("한국 국채 3년", "IRR_GOVT03Y") }

        val cnbcBonds = cnbcD.await()

        MarketData(
            stockIndices = linkedMapOf(
                "KOSPI" to kospiD.await(),
                "NASDAQ" to nasdaqD.await(),
                "S&P500" to sp500D.await()
            ),
            exchangeRates = linkedMapOf(
                "USD/KRW" to usdD.await(),
                "EUR/KRW" to eurD.await(),
                "JPY/KRW" to jpyD.await(),
                "DXY" to dxyD.await()
            ),
            bondRates = linkedMapOf(
                "한국 국채 10년" to (cnbcBonds["KR10Y-KR"] ?: BondData("한국 국채 10년", "N/A")),
                "한국 국채 3년" to kr3yD.await(),
                "미국 국채 10년" to (cnbcBonds["US10Y-US"] ?: BondData("미국 국채 10년", "N/A")),
                "미국 국채 2년" to (cnbcBonds["US2Y-US"] ?: BondData("미국 국채 2년", "N/A"))
            )
        )
    }

    // ── Yahoo Finance ────────────────────────────────────────────────────────

    private fun yahooIndex(name: String, symbol: String): IndexData = runCatching {
        val (price, change, pct) = yahooQuote(symbol)
        IndexData(name, fmtNumber(price), fmtChange(change), fmtPct(pct))
    }.getOrElse {
        log.warn("[Market] $name ($symbol): ${it.message}")
        IndexData(name, "N/A", "N/A", "N/A")
    }

    private fun yahooFx(name: String, symbol: String): ExchangeData = runCatching {
        val (price, change, _) = yahooQuote(symbol)
        val value = if (name.endsWith("/KRW")) fmtNumber(price) else fmtDecimal(price)
        ExchangeData(name, value, fmtChange(change))
    }.getOrElse {
        log.warn("[Market] $name ($symbol): ${it.message}")
        ExchangeData(name, "N/A", "N/A")
    }

    private fun yahooQuote(symbol: String): Triple<Double, Double, Double> {
        val encoded = symbol.replace("^", "%5E")
        val body = get("https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=1d&range=1d")
        val meta = objectMapper.readTree(body)
            .path("chart").path("result").firstOrNull()?.path("meta")
            ?: error("result 없음: $symbol")

        val price = meta.path("regularMarketPrice").asDouble(Double.NaN)
        val prevClose = meta.path("chartPreviousClose").asDouble(Double.NaN)
        check(!price.isNaN() && !prevClose.isNaN() && prevClose != 0.0) {
            "유효하지 않은 가격: $symbol"
        }
        return Triple(price, price - prevClose, (price - prevClose) / prevClose * 100.0)
    }

    // ── CNBC Quote API ───────────────────────────────────────────────────────

    /**
     * CNBC API로 KR10Y-KR / US10Y-US / US2Y-US 한 번에 조회
     * last 필드: "3.916%" 형태 → 그대로 표시
     */
    private fun fetchCnbcBonds(): Map<String, BondData> {
        val displayNames = mapOf(
            "KR10Y-KR" to "한국 국채 10년",
            "US10Y-US" to "미국 국채 10년",
            "US2Y-US" to "미국 국채 2년"
        )
        return try {
            val symbols = displayNames.keys.joinToString("%7C")
            val url = "https://quote.cnbc.com/quote-html-webservice/restQuote/symbolType/symbol" +
                    "?symbols=$symbols&requestMethod=itv&noform=1&partnerId=2&fund=1&exthrs=1&output=json&events=1"
            val body = get(url)
            val quotes = objectMapper.readTree(body)
                .path("FormattedQuoteResult").path("FormattedQuote")

            val result = mutableMapOf<String, BondData>()
            quotes.forEach { q ->
                val sym = q.path("symbol").asText()
                val last = q.path("last").asText()
                if (sym.isNotEmpty() && last.isNotEmpty() && last != "null") {
                    val displayName = displayNames[sym] ?: sym
                    // last 값이 이미 "3.916%" 형태이므로 그대로 사용
                    result[sym] = BondData(displayName, last)
                }
            }
            result
        } catch (e: Exception) {
            log.warn("[Market] CNBC 국채 조회 실패: ${e.message}")
            emptyMap()
        }
    }

    // ── Naver Finance (한국 3년 국채) ────────────────────────────────────────

    /**
     * Naver Finance interestDailyQuote HTML에서 금리 파싱 (EUC-KR)
     * marketindexCd: IRR_GOVT03Y 등
     */
    private fun naverBond(name: String, code: String): BondData = runCatching {
        val url = "https://finance.naver.com/marketindex/interestDailyQuote.naver?marketindexCd=$code&page=1"
        val raw = getRaw(url, mapOf("Referer" to "https://finance.naver.com/marketindex/"))
        val html = raw.toString(java.nio.charset.Charset.forName("EUC-KR"))

        // class="num">3.58 형태에서 첫 번째 숫자 추출
        val match = Regex("""class="num">([0-9.]+)""").find(html)
            ?: error("금리 파싱 실패: $code")
        val rate = match.groupValues[1]
        BondData(name, "$rate%")
    }.getOrElse {
        log.warn("[Market] $name ($code) Naver: ${it.message}")
        BondData(name, "N/A")
    }

    // ── HTTP 헬퍼 ────────────────────────────────────────────────────────────

    private fun get(url: String, extraHeaders: Map<String, String> = emptyMap()): String =
        getRaw(url, extraHeaders).toString(Charsets.UTF_8)

    private fun getRaw(url: String, extraHeaders: Map<String, String> = emptyMap()): ByteArray {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "Mozilla/5.0 (compatible; OurHomeService/1.0)")
            .header("Accept", "*/*")
        extraHeaders.forEach { (k, v) -> builder.header(k, v) }
        return http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body()
    }

    // ── 포맷 헬퍼 ────────────────────────────────────────────────────────────

    private fun fmtNumber(v: Double) = String.format("%,.2f", v)
    private fun fmtDecimal(v: Double) = String.format("%.2f", v)
    private fun fmtChange(v: Double) = (if (v >= 0) "+" else "") + String.format("%.2f", v)
    private fun fmtPct(v: Double) = (if (v >= 0) "+" else "") + String.format("%.2f", v) + "%"
}
