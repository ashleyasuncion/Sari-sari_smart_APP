package com.example.sari_sari_smart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sari_sari_smart.data.RestockLogEntry
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "restock_log")
data class RestockLogEntity(
    @PrimaryKey val id: String,
    val date: String,
    val itemsJson: String, // JSON array of purchase entries
    val totalCost: Double
) {
    fun toDomainModel(): RestockLogEntry {
        val items = mutableListOf<com.example.sari_sari_smart.data.PurchaseEntry>()
        try {
            val arr = JSONArray(itemsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(com.example.sari_sari_smart.data.PurchaseEntry(
                    productId = obj.optString("productId", null),
                    productEntityId = obj.optInt("productEntityId", 0),
                    productName = obj.optString("productName", ""),
                    costPerUnit = obj.optDouble("costPerUnit", 0.0),
                    qtyAdded = obj.optInt("qtyAdded", 0),
                    totalCost = obj.optDouble("totalCost", 0.0)
                ))
            }
        } catch (_: Exception) {}
        return RestockLogEntry(
            id = id,
            date = date,
            items = items,
            totalCost = totalCost
        )
    }

    companion object {
        fun fromDomainModel(entry: RestockLogEntry): RestockLogEntity {
            val arr = JSONArray()
            entry.items.forEach { item ->
                arr.put(JSONObject().apply {
                    item.productId?.let { put("productId", it) }
                    if (item.productEntityId > 0) put("productEntityId", item.productEntityId)
                    put("productName", item.productName)
                    put("costPerUnit", item.costPerUnit)
                    put("qtyAdded", item.qtyAdded)
                    put("totalCost", item.totalCost)
                })
            }
            return RestockLogEntity(
                id = entry.id,
                date = entry.date,
                itemsJson = arr.toString(),
                totalCost = entry.totalCost
            )
        }
    }
}
