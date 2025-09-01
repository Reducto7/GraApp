package com.example.gra.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.gra.ui.data.ExerciseEntity
import com.example.gra.ui.data.ExerciseRepository
import com.example.gra.ui.data.Remote
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ExerciseViewModel(app: Application) : AndroidViewModel(app) {

    // 2) 成员
    private val remote = Remote.create()

    private val repo = ExerciseRepository.create(app)

    private val LABEL_FAVORITES = "收藏"
    private val LABEL_ALL = "全部"
    private val LABEL_CUSTOM = "自定义" // UI 可用这个分类露出“新增自定义运动”按钮

    private val _search = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null) // null=全部
    private val _showFavorites = MutableStateFlow(false)

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    /** 分类（来自 DB） */
    val categories: StateFlow<List<String>> =
        repo.categories()
            .map { it.sorted() }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private data class QueryState(
        val q: String, val cat: String?, val favMode: Boolean, val favIds: Set<String>
    )

    /** 列表数据（分页） */
    val exercisesPaged: Flow<PagingData<ExerciseEntity>> =
        combine(_search, _selectedCategory, _showFavorites, _favoriteIds) { q, cat, fav, ids ->
            QueryState(q.trim(), cat, fav, ids)
        }.flatMapLatest { s ->
            when {
                s.favMode -> repo.favorites(s.favIds, s.q)
                s.q.isNotEmpty() || s.cat != null -> repo.list(s.q, s.cat)
                else -> repo.list("", null) // 全部
            }
        }.cachedIn(viewModelScope)

    fun updateSearch(text: String) { _search.value = text }

    /** 传入：收藏/全部/自定义/具体分类 */
    fun setModeByLabel(label: String) {
        when (label) {
            LABEL_FAVORITES -> { _showFavorites.value = true;  _selectedCategory.value = null }
            LABEL_ALL, LABEL_CUSTOM -> { _showFavorites.value = false; _selectedCategory.value = null }
            else -> { _showFavorites.value = false; _selectedCategory.value = label }
        }
    }

    // 3) 收藏监听：替换 startFavoritesListener()
    fun startFavoritesListener(userId: String) {
        if (userId.isBlank()) return
        remote.observeExerciseFavorites(userId)
            .onEach { ids -> _favoriteIds.value = ids }
            .launchIn(viewModelScope)
    }

    private fun isFavorite(id: String) = _favoriteIds.value.contains(id)

    // 4) 收藏切换：替换 toggleFavorite()
    fun toggleFavorite(
        userId: String,
        exercise: ExerciseEntity,
        onSuccess: (Boolean) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (userId.isBlank()) { onError(IllegalStateException("未登录")); return }
        val has = _favoriteIds.value.contains(exercise.id)
        viewModelScope.launch {
            try {
                if (has) {
                    remote.removeExerciseFavorite(userId, exercise.id)
                    _favoriteIds.value = _favoriteIds.value - exercise.id
                    onSuccess(false)
                } else {
                    remote.addExerciseFavorite(userId, exercise.id, exercise.name)
                    _favoriteIds.value = _favoriteIds.value + exercise.id
                    onSuccess(true)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // ============ 选择并累计（运动） ============

    data class SelectedExercise(val name: String, val minutes: Int, val kcal: Int)

    private val _selectedItems = MutableStateFlow<List<SelectedExercise>>(emptyList())
    val selectedItemsFlow: StateFlow<List<SelectedExercise>> = _selectedItems.asStateFlow()

    val selectedItems: List<SelectedExercise> get() = _selectedItems.value

    fun addExercise(ex: ExerciseEntity, minutes: Int, weightKg: Double = 60.0) {
        val kcal = ((ex.met * 3.5 * weightKg * minutes) / 200.0).roundToInt()
        _selectedItems.value = _selectedItems.value + SelectedExercise(ex.name, minutes, kcal)
    }

    fun addCustomExercise(name: String, minutes: Int, kcal: Int) {
        val n = name.ifBlank { "사용자 정의 운동" }
        _selectedItems.value = _selectedItems.value + SelectedExercise(n, minutes, kcal)
    }

    fun removeSelectedAt(index: Int) {
        val cur = _selectedItems.value
        if (index !in cur.indices) return
        _selectedItems.value = cur.toMutableList().also { it.removeAt(index) }
    }

    fun clearAll() { _selectedItems.value = emptyList() }

    fun totalItemsCount(): Int = _selectedItems.value.size
    fun totalItemsKcal(): Int = _selectedItems.value.sumOf { it.kcal }


    // ============ 保存到 Firestore（写入消耗） ============

    // 5) 保存消耗：替换 saveBurn()
    fun saveBurn(
        userId: String,
        date: String,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (userId.isBlank()) { onError(IllegalStateException("未登录")); return }
        if (selectedItems.isEmpty()) { onError(IllegalStateException("未添加任何运动")); return }
        viewModelScope.launch {
            try {
                val payload = selectedItems.map { Remote.ExerciseUpload(it.name, it.minutes, it.kcal) }
                remote.appendExercises(userId, date, payload)
                remote.markTaskCompleted(userId, date, Remote.TaskId.WORKOUT)
                clearAll()
                onComplete()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
