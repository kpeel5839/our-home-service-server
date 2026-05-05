package com.ourhome.server.config

import com.ourhome.server.domain.auth.JwtClaims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap

class DuplicateRequestFilter : OncePerRequestFilter() {

    // key → last request timestamp (ms)
    private val cache = ConcurrentHashMap<String, Long>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        if (request.method == "GET") {
            chain.doFilter(request, response)
            return
        }

        val claims = SecurityContextHolder.getContext().authentication?.principal as? JwtClaims
        val userId = claims?.memberId ?: request.remoteAddr
        val key = "$userId:${request.method}:${request.requestURI}"
        val now = System.currentTimeMillis()

        // 메모리 누수 방지: 캐시가 커지면 2초 이상 된 항목 제거
        if (cache.size > 500) {
            cache.entries.removeIf { now - it.value > 2000 }
        }

        val last = cache.put(key, now)
        if (last != null && now - last < 1000) {
            response.status = 429
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"message":"요청이 너무 빠릅니다. 잠시 후 다시 시도해주세요.","code":"TOO_MANY_REQUESTS"}""")
            return
        }

        chain.doFilter(request, response)
    }
}
