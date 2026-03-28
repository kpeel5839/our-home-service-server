package com.ourhome.server.domain.member

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/members")
class MemberController(private val memberRepository: MemberRepository) {

    @GetMapping
    fun getMembers(): ResponseEntity<List<MemberResponse>> =
        ResponseEntity.ok(memberRepository.findAll().map { it.toResponse() })
}
