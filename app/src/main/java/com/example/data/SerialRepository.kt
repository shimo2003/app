package com.example.data

import android.content.Context
import com.example.model.SerialBatch
import com.example.model.SerialItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class SerialRepository(private val context: Context) {

    private val metaFile = File(context.filesDir, "serial_batches.json")
    private val _batches = MutableStateFlow<List<SerialBatch>>(emptyList())
    val batches: StateFlow<List<SerialBatch>> = _batches.asStateFlow()

    init {
        loadBatches()
        if (_batches.value.isEmpty()) {
            seedSampleSerialBatch()
        }
    }

    private fun loadBatches() {
        if (!metaFile.exists()) return
        try {
            val jsonStr = metaFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<SerialBatch>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val batchId = obj.getString("id")
                val title = obj.getString("title")
                val createdAt = obj.getLong("createdAt")
                val updatedAt = obj.getLong("updatedAt")

                val itemsArray = obj.getJSONArray("items")
                val itemsList = mutableListOf<SerialItem>()

                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    itemsList.add(
                        SerialItem(
                            id = itemObj.getString("id"),
                            serialNumber = itemObj.getString("serialNumber"),
                            label = itemObj.optString("label", "Hardware Asset"),
                            category = itemObj.optString("category", "Inventory"),
                            scannedAt = itemObj.optLong("scannedAt", System.currentTimeMillis()),
                            status = itemObj.optString("status", "Verified"),
                            notes = itemObj.optString("notes", "")
                        )
                    )
                }

                list.add(
                    SerialBatch(
                        id = batchId,
                        title = title,
                        items = itemsList,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
            }
            _batches.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBatches() {
        try {
            val jsonArray = JSONArray()
            for (batch in _batches.value) {
                val obj = JSONObject().apply {
                    put("id", batch.id)
                    put("title", batch.title)
                    put("createdAt", batch.createdAt)
                    put("updatedAt", batch.updatedAt)

                    val itemsArray = JSONArray()
                    for (item in batch.items) {
                        val itemObj = JSONObject().apply {
                            put("id", item.id)
                            put("serialNumber", item.serialNumber)
                            put("label", item.label)
                            put("category", item.category)
                            put("scannedAt", item.scannedAt)
                            put("status", item.status)
                            put("notes", item.notes)
                        }
                        itemsArray.put(itemObj)
                    }
                    put("items", itemsArray)
                }
                jsonArray.put(obj)
            }
            metaFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createBatch(title: String, initialItems: List<SerialItem> = emptyList()): SerialBatch {
        val newBatch = SerialBatch(
            id = UUID.randomUUID().toString(),
            title = title,
            items = initialItems,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _batches.value = listOf(newBatch) + _batches.value
        saveBatches()
        return newBatch
    }

    fun updateBatch(batch: SerialBatch) {
        val updated = batch.copy(updatedAt = System.currentTimeMillis())
        _batches.value = _batches.value.map { if (it.id == batch.id) updated else it }
        saveBatches()
    }

    fun deleteBatch(batchId: String) {
        _batches.value = _batches.value.filter { it.id != batchId }
        saveBatches()
    }

    fun addSerialItem(batchId: String, item: SerialItem) {
        val current = _batches.value.find { it.id == batchId } ?: return
        val updated = current.copy(
            items = current.items + item,
            updatedAt = System.currentTimeMillis()
        )
        updateBatch(updated)
    }

    fun removeSerialItem(batchId: String, itemId: String) {
        val current = _batches.value.find { it.id == batchId } ?: return
        val updated = current.copy(
            items = current.items.filter { it.id != itemId },
            updatedAt = System.currentTimeMillis()
        )
        updateBatch(updated)
    }

    private fun seedSampleSerialBatch() {
        val sampleItems = listOf(
            SerialItem(
                serialNumber = "SN-DELL-99214-XPS",
                label = "Dell XPS 15 Workstation",
                category = "Laptops",
                status = "Verified",
                notes = "Core i9 / 32GB RAM / 1TB NVMe"
            ),
            SerialItem(
                serialNumber = "SN-APPL-C02Z8918MD6",
                label = "MacBook Pro 16\" M3 Max",
                category = "Laptops",
                status = "Verified",
                notes = "Assigned to Engineering Lead"
            ),
            SerialItem(
                serialNumber = "SN-CSCO-2960X-48TS",
                label = "Cisco Catalyst 2960-X Switch",
                category = "Networking",
                status = "Verified",
                notes = "Rack 2, Port 1-48 Gigabit"
            ),
            SerialItem(
                serialNumber = "SN-LG-34WK95U-0941",
                label = "LG UltraWide 34\" 5K Monitor",
                category = "Displays",
                status = "Verified",
                notes = "Studio workstation monitor"
            ),
            SerialItem(
                serialNumber = "SN-ZEB-TC520K-8812",
                label = "Zebra TC52 Touch Computer",
                category = "Scanners",
                status = "In-Service",
                notes = "Warehouse barcode & inventory terminal"
            )
        )

        createBatch("IT Hardware Asset Audit Q4", sampleItems)
    }
}
