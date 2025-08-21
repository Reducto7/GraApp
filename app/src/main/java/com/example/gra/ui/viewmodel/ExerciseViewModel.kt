package com.example.gra.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.gra.ui.data.ExerciseEntity
import com.example.gra.ui.data.ExerciseRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

class ExerciseViewModel(app: Application) : AndroidViewModel(app) {

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

    // ============ 收藏（exercise_favorites） ============

    fun startFavoritesListener(userId: String) {
        if (userId.isBlank()) return
        Firebase.firestore.collection("users").document(userId)
            .collection("exercise_favorites")
            .addSnapshotListener { snap, e ->
                if (e != null) { Log.e("ExFav", "listen error", e); return@addSnapshotListener }
                val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                Log.i("ExFav", "fav ids updated size=${ids.size}")
                _favoriteIds.value = ids
            }
    }

    private fun isFavorite(id: String) = _favoriteIds.value.contains(id)

    /** 切换收藏（乐观更新+回调） */
    fun toggleFavorite(
        userId: String,
        ex: ExerciseEntity,
        onSuccess: (added: Boolean) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (userId.isBlank()) { onError(IllegalStateException("未登录")); return }
        val doc = Firebase.firestore.collection("users").document(userId)
            .collection("exercise_favorites").document(ex.id)

        if (isFavorite(ex.id)) {
            doc.delete()
                .addOnSuccessListener {
                    _favoriteIds.value = _favoriteIds.value - ex.id
                    onSuccess(false)
                }
                .addOnFailureListener { onError(it) }
        } else {
            doc.set(mapOf(
                "name" to ex.name,
                "category" to ex.category,
                "ts" to FieldValue.serverTimestamp()
            ))
                .addOnSuccessListener {
                    _favoriteIds.value = _favoriteIds.value + ex.id
                    onSuccess(true)
                }
                .addOnFailureListener { onError(it) }
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

    /**
     * 保存到 users/{uid}/records/{date}
     * - 累加 totalBurn
     * - 追加 exercises 数组（每项: name, minutes, kcal）
     */
    fun saveBurn(
        userId: String,
        date: String,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (userId.isBlank()) { onError(IllegalStateException("未登录")); return }
        if (selectedItems.isEmpty()) { onError(IllegalStateException("未添加任何运动")); return }

        val docRef = Firebase.firestore.collection("users").document(userId)
            .collection("records").document(date)

        Firebase.firestore.runTransaction { tr ->
            val snap = tr.get(docRef)
            val oldBurn = snap.getLong("totalBurn")?.toInt() ?: 0
            val add = selectedItems.sumOf { it.kcal }

            // 追加 exercises 多条
            val arrayToAdd = selectedItems.map {
                mapOf("name" to it.name, "minutes" to it.minutes, "kcal" to it.kcal)
            }.toTypedArray()

            tr.set(docRef, mapOf(
                "totalBurn" to (oldBurn + add)
            ), com.google.firebase.firestore.SetOptions.merge())

            tr.update(docRef, mapOf(
                "exercises" to FieldValue.arrayUnion(*arrayToAdd)
            ))
        }.addOnSuccessListener {
            Log.i("FirestoreBurn", "✅ 保存成功: users/$userId/records/$date")
            clearAll()
            onComplete()
        }.addOnFailureListener { e ->
            Log.e("FirestoreBurn", "❌ 保存失败", e)
            onError(e)
        }
    }
}
