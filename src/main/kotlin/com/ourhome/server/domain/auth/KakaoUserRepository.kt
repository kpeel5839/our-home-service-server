package com.ourhome.server.domain.auth

import org.springframework.data.jpa.repository.JpaRepository

interface KakaoUserRepository : JpaRepository<KakaoUser, String>
