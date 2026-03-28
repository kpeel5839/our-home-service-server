package com.ourhome.server.domain.menu

import jakarta.persistence.*
import java.util.UUID

enum class MealType { BREAKFAST, LUNCH, DINNER }

@Entity
@Table(name = "daily_menus")
class DailyMenu(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val date: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val mealType: MealType,

    @Column(nullable = false)
    var menuName: String,

    var description: String? = null,

    @Column(nullable = false)
    val registeredBy: String
)

data class MenuResponse(
    val id: String,
    val date: String,
    val mealType: String,
    val menuName: String,
    val description: String?,
    val registeredBy: String
)

data class CreateMenuRequest(
    val date: String,
    val mealType: MealType,
    val menuName: String,
    val description: String? = null,
    val registeredBy: String
)

data class UpdateMenuRequest(
    val menuName: String? = null,
    val description: String? = null
)

fun DailyMenu.toResponse() = MenuResponse(
    id = id,
    date = date,
    mealType = mealType.name,
    menuName = menuName,
    description = description,
    registeredBy = registeredBy
)
