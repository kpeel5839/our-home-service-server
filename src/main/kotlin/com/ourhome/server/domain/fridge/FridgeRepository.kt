package com.ourhome.server.domain.fridge

import org.springframework.data.jpa.repository.JpaRepository

interface FridgeRepository : JpaRepository<FridgeItem, String> {
    fun findByIsConsumedFalse(): List<FridgeItem>
    fun findByStorageTypeAndIsConsumedFalse(storageType: StorageType): List<FridgeItem>
}
