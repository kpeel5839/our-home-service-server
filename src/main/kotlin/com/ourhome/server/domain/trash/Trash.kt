package com.ourhome.server.domain.trash

import jakarta.persistence.*
import java.util.UUID

enum class TrashType { GENERAL, RECYCLE, FOOD, LARGE }

@Entity
@Table(name = "trash_schedules")
class TrashSchedule(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val date: String,

    @Column(nullable = false)
    val memberId: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trash_schedule_types", joinColumns = [JoinColumn(name = "schedule_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "trash_type")
    val trashTypes: MutableList<TrashType> = mutableListOf(),

    var isCompleted: Boolean = false
)

@Entity
@Table(name = "apartment_trash_rules")
class ApartmentTrashRule(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val dayOfWeek: Int,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trash_rule_types", joinColumns = [JoinColumn(name = "rule_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "trash_type")
    val trashTypes: MutableList<TrashType> = mutableListOf()
)

data class TrashScheduleResponse(
    val id: String,
    val date: String,
    val memberId: String,
    val trashTypes: List<String>,
    val isCompleted: Boolean
)

data class ApartmentTrashRuleResponse(
    val id: String,
    val dayOfWeek: Int,
    val trashTypes: List<String>
)

data class CreateTrashScheduleRequest(
    val date: String,
    val memberId: String,
    val trashTypes: List<TrashType>
)

fun TrashSchedule.toResponse() = TrashScheduleResponse(
    id = id, date = date, memberId = memberId,
    trashTypes = trashTypes.map { it.name }, isCompleted = isCompleted
)

fun ApartmentTrashRule.toResponse() = ApartmentTrashRuleResponse(
    id = id, dayOfWeek = dayOfWeek, trashTypes = trashTypes.map { it.name }
)
