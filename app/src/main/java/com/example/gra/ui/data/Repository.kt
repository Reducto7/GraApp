package com.example.gra.ui.data

import android.app.Application
import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.gra.ui.viewmodel.BodyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FoodRepository(private val db: AppDatabase) {

    fun categories() = db.foodDao().categoriesFlow()

    fun searchContains(q: String): Flow<PagingData<FoodEntity>> =
        Pager(PagingConfig(pageSize = 30)) {
            db.foodDao().searchContainsPaged(q.lowercase())
        }.flow

    fun listByCategory(category: String?): Flow<PagingData<FoodEntity>> =
        Pager(PagingConfig(pageSize = 30)) {
            db.foodDao().listByCategoryPaged(category)
        }.flow

    /** 收藏分页：ids 为空直接返回空分页，避免 SQL IN () 异常 */
    fun favorites(ids: Set<String>, q: String): Flow<PagingData<FoodEntity>> =
        if (ids.isEmpty()) flowOf(PagingData.empty())
        else Pager(PagingConfig(pageSize = 30)) {
            db.foodDao().listByIdsPaged(ids.toList(), q.lowercase())
        }.flow

    suspend fun getByName(name: String) = db.foodDao().getByName(name)

    companion object { fun create(ctx: Context) = FoodRepository(AppDatabase.getInstance(ctx)) }
}

// ✅ 同文件并排新增
class ExerciseRepository(private val db: AppDatabase) {
    fun categories() = db.exerciseDao().categoriesFlow()

    fun list(q: String, category: String?) =
        Pager(PagingConfig(pageSize = 30)) {
            db.exerciseDao().listPaged(category, q.lowercase())
        }.flow

    fun favorites(ids: Set<String>, q: String) =
        if (ids.isEmpty()) flowOf(PagingData.empty())
        else Pager(PagingConfig(pageSize = 30)) {
            db.exerciseDao().listByIdsPaged(ids.toList(), q.lowercase())
        }.flow

    companion object { fun create(ctx: Context) = ExerciseRepository(AppDatabase.getInstance(ctx)) }
}

class BodyMeasureRepository private constructor(
    private val dao: BodyMeasureDao
) {
    companion object {
        // 和其它仓库一致：用 getInstance(context)
        fun create(ctx: Context): BodyMeasureRepository {
            val db = AppDatabase.getInstance(ctx)
            return BodyMeasureRepository(db.bodyMeasureDao())
        }

        // 如果你的 ViewModel 里用的是 Application 类型，也可以重载一个：
        fun create(app: Application): BodyMeasureRepository =
            create(app.applicationContext)
    }

    // —— 这里是更新 / 删除 —— //
    // 有 id 主键的版本（推荐）
    suspend fun updateMeasure(type: BodyType, id: String, value: Double, date: String) {
        dao.updateById(id, value, date)
    }

    suspend fun deleteMeasure(type: BodyType, id: String) {
        dao.deleteById(id)
    }

    // 如果你的表没有独立 id，而是以 (type, date) 为“联合主键”，
    // 可以把上面两个方法替换为下面注释的版本：
    /*
    suspend fun updateMeasure(type: BodyType, id: String, value: Double, date: String) {
        // id 用 "typeKey-YYYY-MM-DD" 兜底时的解析
        val (tKey, oldDate) = parseId(id) // 自己实现 parseId()
        dao.updateByTypeDate(tKey, oldDate, value, date)
    }

    suspend fun deleteMeasure(type: BodyType, id: String) {
        val (tKey, d) = parseId(id)
        dao.deleteByTypeDate(tKey, d)
    }
    */
}

