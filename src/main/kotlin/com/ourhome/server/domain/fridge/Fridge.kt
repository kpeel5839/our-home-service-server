package com.ourhome.server.domain.fridge

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class FridgeCategory { VEGETABLE, MEAT, SEAFOOD, DAIRY, DRINK, SIDE_DISH, SAUCE, OTHER }
enum class StorageType { FRIDGE, FREEZER, ROOM_TEMP }

@Entity
@Table(name = "fridge_items")
class FridgeItem(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val registeredBy: String,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: FridgeCategory,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var unit: String,

    @Column(nullable = false)
    var expirationDate: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var storageType: StorageType,

    var isConsumed: Boolean = false,

    @Column(nullable = false)
    val createdAt: String = Instant.now().toString()
)

data class FridgeItemResponse(
    val id: String, val registeredBy: String, val name: String, val category: String,
    val quantity: Int, val unit: String, val expirationDate: String, val storageType: String,
    val isConsumed: Boolean, val createdAt: String
)

data class CreateFridgeItemRequest(
    val registeredBy: String, val name: String, val category: FridgeCategory,
    val quantity: Int, val unit: String, val expirationDate: String, val storageType: StorageType
)

fun FridgeItem.toResponse() = FridgeItemResponse(
    id, registeredBy, name, category.name, quantity, unit, expirationDate, storageType.name, isConsumed, createdAt
)
