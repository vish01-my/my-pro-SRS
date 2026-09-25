package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commodities")
data class CommodityEntity(
    @PrimaryKey val id: String,                   // "RICE", "WHEAT", "SUGAR", "DAL", "SALT", "OIL"
    val name: String,
    val hindiName: String,
    val unit: String,                             // "kg" or "L"
    val standardPricePerUnit: Double,             // Market reference
    val subsidizedPricePerUnit: Double,           // NFSA price (e.g. ₹3/kg or free)
    val isAvailable: Boolean = true
)

@Entity(tableName = "dealer_inventory")
data class DealerInventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fpsId: String,
    val commodityId: String,
    val commodityName: String,
    val unit: String,
    val currentStock: Double,                     // in kg or L
    val minThreshold: Double,                     // buffer alert
    val lastRestockedDate: String
)
