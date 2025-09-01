package com.example.gra.ui.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

// ✅ 追加在同文件内
@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT DISTINCT category FROM exercises")
    fun categoriesFlow(): Flow<List<String>>

    @Query("""
        SELECT * FROM exercises
        WHERE (:category IS NULL OR category = :category)
          AND (:q = '' OR name_lc LIKE '%' || :q || '%')
        ORDER BY name_lc
    """)
    fun listPaged(category: String?, q: String): PagingSource<Int, ExerciseEntity>

    @Query("""
        SELECT * FROM exercises
        WHERE id IN (:ids)
          AND (:q = '' OR name_lc LIKE '%' || :q || '%')
        ORDER BY name_lc
    """)
    fun listByIdsPaged(ids: List<String>, q: String): PagingSource<Int, ExerciseEntity>
}

@Dao
interface BodyMeasureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BodyMeasureEntity)

    @Query("SELECT * FROM body_measures WHERE type = :type ORDER BY date ASC")
    fun history(type: String): Flow<List<BodyMeasureEntity>>

    @Query("""
        SELECT * FROM body_measures 
        WHERE type = :type 
        ORDER BY date DESC 
        LIMIT 1
    """)
    fun latest(type: String): Flow<BodyMeasureEntity?>

    @Query("UPDATE body_measures SET value = :value, date = :date WHERE id = :id")
    suspend fun updateById(id: String, value: Double, date: String)

    @Query("DELETE FROM body_measures WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE body_measures SET value = :value, date = :newDate WHERE type = :type AND date = :oldDate")
    suspend fun updateByTypeDate(type: String, oldDate: String, value: Double, newDate: String)

    @Query("DELETE FROM body_measures WHERE type = :type AND date = :date")
    suspend fun deleteByTypeDate(type: String, date: String)

    @Query("SELECT * FROM body_measures WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): BodyMeasureEntity?
}
