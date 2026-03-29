package com.ourhome.server.config

import com.ourhome.server.domain.auth.JwtClaims
import com.ourhome.server.domain.auth.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val token = resolveToken(request)
        if (token == null) {
            log.debug("[JwtAuthFilter] 토큰 없음 - ${request.method} ${request.requestURI}")
        } else if (!jwtService.isValid(token)) {
            log.warn("[JwtAuthFilter] 토큰 유효하지 않음 - ${request.method} ${request.requestURI}")
        } else {
            val claims = jwtService.parse(token)
            val auth = UsernamePasswordAuthenticationToken(
                claims,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER"))
            ).also { it.details = WebAuthenticationDetailsSource().buildDetails(request) }
            SecurityContextHolder.getContext().authentication = auth
            log.debug("[JwtAuthFilter] 인증 성공 - ${claims.kakaoId} → ${request.requestURI}")
        }
        chain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.removePrefix("Bearer ").trim()
    }
}
