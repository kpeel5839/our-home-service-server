package com.ourhome.server.domain.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiry-ms}") private val expiryMs: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generate(kakaoId: String, memberId: String?): String = Jwts.builder()
        .subject(kakaoId)
        .apply { memberId?.let { claim("memberId", it) } }
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + expiryMs))
        .signWith(key)
        .compact()

    fun parse(token: String): JwtClaims {
        val claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
        return JwtClaims(
            kakaoId = claims.subject,
            memberId = claims["memberId"] as? String
        )
    }

    fun isValid(token: String): Boolean = runCatching { parse(token) }.isSuccess
}

data class JwtClaims(val kakaoId: String, val memberId: String?)
