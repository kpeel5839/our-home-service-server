package com.ourhome.server.domain.auth

import org.springframework.data.jpa.repository.JpaRepository

interface LoginApprovalRepository : JpaRepository<LoginApproval, String> {
    fun findByStatusOrderByRequestedAtDesc(status: ApprovalStatus): List<LoginApproval>
}
