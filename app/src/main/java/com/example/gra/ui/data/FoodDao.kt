package com.example.gra.ui.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FoodEntity>)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    // 分类流（左侧列表的数据）
    @Query("SELECT DISTINCT category FROM foods")
    fun categoriesFlow(): kotlinx.coroutines.flow.Flow<List<String>>

    // ✅ 包含匹配（LIKE %q%）
    @Query("""
        SELECT * FROM foods
        WHERE name_lc LIKE '%' || :q || '%'
        ORDER BY name_lc
    """)
    fun searchContainsPaged(q: String): androidx.paging.PagingSource<Int, FoodEntity>

    // ✅ 按分类；category 为 null 表示“全部”
    @Query("""
        SELECT * FROM foods
        WHERE (:category IS NULL OR category = :category)
        ORDER BY name_lc
    """)
    fun listByCategoryPaged(category: String?): androidx.paging.PagingSource<Int, FoodEntity>

    @Query("""
    SELECT * FROM foods
    WHERE id IN (:ids)
      AND (:q = '' OR name_lc LIKE '%' || :q || '%')
    ORDER BY name_lc
""")
    fun listByIdsPaged(
        ids: List<String>,   // ⚠️ 如果你的 id 是 Int，改成 List<Int>
        q: String
    ): PagingSource<Int, FoodEntity>


    // 可选：按名字取一条（旧 addFood 若需要）
    @Query("SELECT * FROM foods WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): FoodEntity?
}

