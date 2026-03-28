package com.ourhome.server.domain.fridge

import com.ourhome.server.config.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/fridge")
class FridgeController(private val fridgeRepository: FridgeRepository) {

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
        val item = FridgeItem(
            registeredBy = request.registeredBy,
            name = request.name,
            category = request.category,
            quantity = request.quantity,
            unit = request.unit,
            expirationDate = request.expirationDate,
            storageType = request.storageType
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(fridgeRepository.save(item).toResponse())
    }

    @PatchMapping("/{id}/consume")
    fun consumeItem(@PathVariable id: String): ResponseEntity<FridgeItemResponse> {
        val item = fridgeRepository.findById(id).orElseThrow { NotFoundException("Item not found: $id") }
        item.isConsumed = true
        return ResponseEntity.ok(fridgeRepository.save(item).toResponse())
    }

    @DeleteMapping("/{id}")
    fun deleteItem(@PathVariable id: String): ResponseEntity<Void> {
        if (!fridgeRepository.existsById(id)) throw NotFoundException("Item not found: $id")
        fridgeRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
