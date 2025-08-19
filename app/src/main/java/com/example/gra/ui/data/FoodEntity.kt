package com.example.gra.ui.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foods",
    indices = [
        Index(value = ["name_lc"]),
        Index(value = ["category"])
    ]
)
data class FoodEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "name") val name: String,       // 식품명_정규화
    @ColumnInfo(name = "state") val state: String,     // 상태
    @ColumnInfo(name = "name_lc") val nameLc: String,  // 小写搜索键
    @ColumnInfo(name = "kcal_100g") val kcal100g: Double?,
    @ColumnInfo(name = "protein_g_100g") val protein100g: Double?,
    @ColumnInfo(name = "fat_g_100g") val fat100g: Double?,
    @ColumnInfo(name = "carb_g_100g") val carb100g: Double?,
    @ColumnInfo(name = "sugar_g_100g") val sugar100g: Double?,
    @ColumnInfo(name = "synonyms") val synonyms: String?
)
