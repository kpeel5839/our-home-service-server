package com.ourhome.server.domain.member

import jakarta.persistence.*
import java.util.UUID

enum class MemberRole { FATHER, MOTHER, SON, DAUGHTER, OTHER }

@Entity
@Table(name = "family_members")
class FamilyMember(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: MemberRole,

    @Column(nullable = false)
    val avatarColor: String,

    val avatarUrl: String? = null
)

data class MemberResponse(
    val id: String,
    val name: String,
    val role: String,
    val avatarColor: String,
    val avatarUrl: String?
)

fun FamilyMember.toResponse() = MemberResponse(
    id = id,
    name = name,
    role = role.name,
    avatarColor = avatarColor,
    avatarUrl = avatarUrl
)
