package com.example.model

import java.util.UUID

data class SerialItem(
    val id: String = UUID.randomUUID().toString(),
    val serialNumber: String,
    val label: String = "Hardware Asset",
    val category: String = "Inventory",
    val scannedAt: Long = System.currentTimeMillis(),
    val status: String = "Verified",
    val notes: String = ""
)

data class SerialBatch(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val items: List<SerialItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val itemCount: Int get() = items.size
}
