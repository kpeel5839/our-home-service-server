package com.ourhome.server.config

import com.ourhome.server.domain.auth.JwtClaims
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {
    fun currentMemberId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        val claims = auth?.principal as? JwtClaims
        return claims?.memberId
            ?: throw IllegalStateException("인증 정보에 memberId가 없습니다. 가족 구성원으로 등록되지 않은 계정입니다.")
    }
}
