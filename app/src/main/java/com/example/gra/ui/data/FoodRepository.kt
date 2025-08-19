package com.example.gra.ui.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
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

