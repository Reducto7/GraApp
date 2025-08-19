package com.example.gra.ui.viewmodel // 替换成你的包名

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.gra.ui.data.FoodRepository
import com.example.gra.ui.data.FoodEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SelectedFood(
    val name: String,
    val grams: Double,
    val kcal: Int
)


data class Meal(
    val name: String,
    val kcal: Int
)

class FoodViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FoodRepository.create(app)

    private val LABEL_FAVORITES = "收藏"
    private val LABEL_ALL = "全部"

    private val _search = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null) // null=全部
    private val _showFavorites = MutableStateFlow(false)

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    val categories: StateFlow<List<String>> =
        repo.categories().map { it.sorted() }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private data class QueryState(
        val q: String,
        val cat: String?,                // null=全部
        val favMode: Boolean,
        val favIds: Set<String>
    )

    val foodsPaged: Flow<PagingData<FoodEntity>> =
        combine(_search, _selectedCategory, _showFavorites, _favoriteIds) { q, cat, fav, ids ->
            QueryState(q.trim(), cat, fav, ids)
        }.flatMapLatest { s ->
            if (s.favMode) {
                Log.i("FavDebug", "enter favorites mode, ids=${s.favIds.size}, q='${s.q}'")
                repo.favorites(s.favIds, s.q)
            } else if (s.q.isNotEmpty()) {
                repo.searchContains(s.q)
            } else {
                repo.listByCategory(s.cat)   // cat = null => 全部
            }
        }.cachedIn(viewModelScope)


    fun updateSearch(text: String) { _search.value = text }

    /** 左侧点击的标签 -> 设置模式 */
    fun setModeByLabel(label: String) {
        when (label) {
            LABEL_FAVORITES -> { _showFavorites.value = true;  _selectedCategory.value = null }
            LABEL_ALL       -> { _showFavorites.value = false; _selectedCategory.value = null }
            else            -> { _showFavorites.value = false; _selectedCategory.value = label }
        }
    }

    // ---------- 收藏（Firestore） ----------
    fun startFavoritesListener(userId: String) {
        if (userId.isBlank()) return
        Firebase.firestore.collection("users").document(userId)
            .collection("favorites")
            .addSnapshotListener { snap, e ->
                if (e != null) { Log.e("FavDebug", "listen error", e); return@addSnapshotListener }
                val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                Log.i("FavDebug", "fav ids updated, size=${ids.size}, sample=${ids.take(3)}")
                _favoriteIds.value = ids
            }
    }



    fun isFavorite(id: String) = _favoriteIds.value.contains(id)

    fun toggleFavorite(
        userId: String,
        food: FoodEntity,
        onSuccess: (added: Boolean) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (userId.isBlank()) {
            onError(IllegalStateException("未登录"))
            return
        }
        val doc = Firebase.firestore.collection("users").document(userId)
            .collection("favorites").document(food.id)

        val current = _favoriteIds.value

        if (current.contains(food.id)) {
            doc.delete()
                .addOnSuccessListener {
                    // ✅ 乐观更新：本地先改
                    _favoriteIds.value = _favoriteIds.value - food.id
                    onSuccess(false)
                }
                .addOnFailureListener { onError(it) }
        } else {
            doc.set(mapOf(
                "name" to food.name,
                "ts" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
                .addOnSuccessListener {
                    // ✅ 乐观更新：本地先改
                    _favoriteIds.value = _favoriteIds.value + food.id
                    onSuccess(true)
                }
                .addOnFailureListener { onError(it) }
        }
    }




    // ---------- 已选项目 / 真实热量 ----------
    var selectedItems: List<SelectedFood> = emptyList()
        private set

    /** 传实体，直接用 kcal100g 计算 */
    fun addFood(food: FoodEntity, grams: Double) {
        val per100 = food.kcal100g ?: 0.0
        val kcal = ((per100 * grams) / 100.0).roundToInt()
        selectedItems = selectedItems + SelectedFood(food.name, grams, kcal)
    }

    fun clearAll() { selectedItems = emptyList() }

    // … 你的 getTodayMealIndex / saveMeal / loadDataByDate 原样保留 …

    // --- UI 状态 ---
    val search: StateFlow<String> = _search
    val selectedCategory: StateFlow<String?> = _selectedCategory



    // 供页面调用
    fun chooseCategory(cat: String?) { _selectedCategory.value = cat }

    /** 用数据库里的 kcal/100g 计算并添加 */
    fun addFood(name: String, grams: Double) {
        viewModelScope.launch {
            val item = repo.getByName(name)
            val per100 = item?.kcal100g ?: 0.0
            val kcal = ((per100 * grams) / 100.0).roundToInt()
            selectedItems = selectedItems + SelectedFood(name, grams, kcal)
        }
    }


    fun addMeal(kcal: Int) {
        val mealName = "第${meals.size + 1}餐"
        meals.add(Meal(mealName, kcal))
        totalIntakeKcal.value += kcal
    }

    // ✅ 每天所有餐次
    var meals = mutableStateListOf<Meal>()
        private set

    // ✅ 当日总摄入 kcal
    var totalIntakeKcal = mutableStateOf(0)
        private set

    // ✅ 如果后期有运动消耗，也可以配一个
    var totalBurnKcal = mutableStateOf(0)
        private set

    /**
     * 总项数
     */
    fun totalItemsCount(): Int = selectedItems.size

    /**
     * 总卡路里
     */
    fun totalItemsKcal(): Int = selectedItems.sumOf { it.kcal }


    /**
     * 从 Firestore 获取今天已有几餐
     * 👉 页面: LaunchedEffect(Unit) { ... }
     */
    fun getTodayMealIndex(
        userId: String,
        date: String,
        onResult: (Int) -> Unit
    ) {
        val docRef = Firebase.firestore
            .collection("users")
            .document(userId)
            .collection("records")
            .document(date)

        docRef.get().addOnSuccessListener { snapshot ->
            val meals = snapshot.get("meals") as? List<*>
            val mealIndex = if (meals != null) meals.size + 1 else 1
            onResult(mealIndex)
        }.addOnFailureListener {
            onResult(1)
        }
    }

    /**
     * 保存到 Firestore
     * ✅ 会把现有 selectedItems 写进去
     * ✅ 会自动计算 totalCalories 并追加
     */
    fun saveMeal(
        userId: String,
        date: String,
        mealIndex: Int,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (selectedItems.isEmpty()) return

        val meal = mapOf(
            "mealIndex" to mealIndex,
            "foods" to selectedItems.map { mapOf("name" to it.name, "grams" to it.grams, "kcal" to it.kcal) }
        )

        val docRef = Firebase.firestore
            .collection("users").document(userId)
            .collection("records").document(date)

        Firebase.firestore.runTransaction { tr ->
            val snap = tr.get(docRef)
            val meals = snap.get("meals") as? ArrayList<Map<String, Any>> ?: arrayListOf()
            meals.add(meal)
            val newTotal = meals.sumOf { m ->
                (m["foods"] as? List<Map<String, Any>> ?: emptyList())
                    .sumOf { (it["kcal"] as? Number)?.toInt() ?: 0 }
            }
            tr.set(docRef, mapOf("meals" to meals, "totalCalories" to newTotal))
        }.addOnSuccessListener {
            clearAll()
            onComplete()
        }.addOnFailureListener { e ->
            e.printStackTrace()
            onError(e)
        }
    }


    fun loadDataByDate(date: LocalDate) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val docRef = Firebase.firestore
            .collection("users")
            .document(userId)
            .collection("records")
            .document(date.toString())

        docRef.get().addOnSuccessListener { snapshot ->
            val mealsList = snapshot.get("meals") as? List<Map<String, Any>> ?: emptyList()

            // 重置
            meals.clear()

            for (meal in mealsList) {
                val foods = meal["foods"] as? List<Map<String, Any>> ?: emptyList()
                val kcal = foods.sumOf { (it["kcal"] as? Number)?.toInt() ?: 0 }
                val mealIndex = meal["mealIndex"]?.toString() ?: "未知餐"
                meals.add(Meal("第${mealIndex}餐", kcal))
            }

            totalIntakeKcal.value = snapshot.getLong("totalCalories")?.toInt() ?: 0

            // 运动数据后期也可从同一个 doc 拉出来
            totalBurnKcal.value = snapshot.getLong("totalBurn")?.toInt() ?: 0
        }.addOnFailureListener {
            println("❌ Firestore 加载失败: ${it.localizedMessage}")
            it.printStackTrace()
        }
    }
}
