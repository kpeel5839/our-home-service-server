package com.ourhome.server.domain.fridge

import com.ourhome.server.config.NotFoundException
import com.ourhome.server.config.SecurityUtils
import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.notification.DiscordNotificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/fridge")
class FridgeController(
    private val fridgeRepository: FridgeRepository,
    private val memberRepository: MemberRepository,
    private val discordNotificationService: DiscordNotificationService
) {

    private val storageLabels = mapOf(
        StorageType.FRIDGE to "냉장",
        StorageType.FREEZER to "냉동",
        StorageType.ROOM_TEMP to "실온"
    )

    @GetMapping
    fun getItems(@RequestParam(required = false) storageType: StorageType?): ResponseEntity<List<FridgeItemResponse>> {
        val items = if (storageType != null) {
            fridgeRepository.findByStorageTypeAndIsConsumedFalse(storageType)
        } else {
            fridgeRepository.findByIsConsumedFalse()
        }
        return ResponseEntity.ok(items.map { it.toResponse() })
    }

    @PostMapping
    fun createItem(@RequestBody request: CreateFridgeItemRequest): ResponseEntity<FridgeItemResponse> {
        val registeredBy = SecurityUtils.currentMemberId()
        val item = FridgeItem(
            registeredBy = registeredBy,
            name = request.name,
            category = request.category,
            quantity = request.quantity,
            unit = request.unit,
            expirationDate = request.expirationDate,
            storageType = request.storageType
        )
        val saved = fridgeRepository.save(item)
        val memberName = memberRepository.findById(registeredBy).map { it.name }.orElse("누군가")
        val storage = storageLabels[request.storageType] ?: request.storageType.name
        discordNotificationService.sendToAllMembers(
            "🧊 ${memberName}님이 ${item.name}을(를) ${storage}에 넣었어요! (${item.quantity}${item.unit}, 유통기한: ${item.expirationDate})"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @PatchMapping("/{id}/consume")
    fun consumeItem(@PathVariable id: String): ResponseEntity<FridgeItemResponse> {
        val item = fridgeRepository.findById(id).orElseThrow { NotFoundException("Item not found: $id") }
        item.isConsumed = true
        val saved = fridgeRepository.save(item)
        val memberName = memberRepository.findById(item.registeredBy).map { it.name }.orElse("누군가")
        discordNotificationService.sendToAllMembers("✅ ${item.name} 소진 완료! (등록자: ${memberName})")
        return ResponseEntity.ok(saved.toResponse())
    }

    @DeleteMapping("/{id}")
    fun deleteItem(@PathVariable id: String): ResponseEntity<Void> {
        val item = fridgeRepository.findById(id).orElseThrow { NotFoundException("Item not found: $id") }
        fridgeRepository.deleteById(id)
        discordNotificationService.sendToAllMembers("🗑️ ${item.name} 냉장고에서 삭제됐어요")
        return ResponseEntity.noContent().build()
    }
}
