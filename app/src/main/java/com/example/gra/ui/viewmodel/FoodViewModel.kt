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
import com.example.gra.ui.data.ExerciseEntity
import com.example.gra.ui.data.ExerciseRepository
import com.example.gra.ui.data.FoodRepository
import com.example.gra.ui.data.FoodEntity
import com.example.gra.ui.data.Remote
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SelectedFood(
    val name: String,
    val grams: Double,
    val kcal: Int
)

// 每餐的条目
data class MealItem(
    val name: String,
    val grams: Double,
    val kcal: Int
)

// 每餐：增加 index（第几餐）和 items（明细列表）
data class Meal(
    val index: Int,
    val name: String,
    val kcal: Int,
    val items: List<MealItem> = emptyList()
)

// —— 运动的 UI/数据模型 —— //
data class ExerciseEntry(
    val index: Int,     // 第几次
    val name: String,
    val minutes: Int,
    val kcal: Int
)




class FoodViewModel(app: Application) : AndroidViewModel(app) {

    // 当天所有运动
    var exercises = mutableStateListOf<ExerciseEntry>()
        private set

    private val remote = Remote.create()
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

    // 3) 收藏监听：替换 startFavoritesListener()
    fun startFavoritesListener(userId: String) {
        if (userId.isBlank()) return
        remote.observeFoodFavorites(userId)
            .onEach { ids -> _favoriteIds.value = ids }
            .launchIn(viewModelScope)
    }

    fun isFavorite(id: String) = _favoriteIds.value.contains(id)

    // 4) 收藏切换：替换 toggleFavorite()
    fun toggleFavorite(
        userId: String,
        food: FoodEntity,
        onSuccess: (added: Boolean) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (userId.isBlank()) { onError(IllegalStateException("未登录")); return }
        val has = _favoriteIds.value.contains(food.id)
        viewModelScope.launch {
            try {
                if (has) {
                    remote.removeFoodFavorite(userId, food.id)
                    _favoriteIds.value = _favoriteIds.value - food.id
                    onSuccess(false)
                } else {
                    remote.addFoodFavorite(userId, food.id, food.name)
                    _favoriteIds.value = _favoriteIds.value + food.id
                    onSuccess(true)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private val _selectedItems = MutableStateFlow<List<SelectedFood>>(emptyList())
    val selectedItemsFlow: StateFlow<List<SelectedFood>> = _selectedItems.asStateFlow()

    // 可选：保留只读兼容（如果你其他地方还直接用 viewModel.selectedItems）
    val selectedItems: List<SelectedFood> get() = _selectedItems.value

    fun addFood(food: FoodEntity, grams: Double) {
        val per100 = food.kcal100g ?: 0.0
        val kcal = ((per100 * grams) / 100.0).roundToInt()
        _selectedItems.value = _selectedItems.value + SelectedFood(food.name, grams, kcal)
    }

    fun removeSelectedAt(index: Int) {
        val cur = _selectedItems.value
        if (index !in cur.indices) return
        _selectedItems.value = cur.toMutableList().also { it.removeAt(index) }
    }

    fun clearAll() { _selectedItems.value = emptyList() }

    fun totalItemsCount(): Int = _selectedItems.value.size
    fun totalItemsKcal(): Int = _selectedItems.value.sumOf { it.kcal }

    // --- UI 状态 ---
    val search: StateFlow<String> = _search
    val selectedCategory: StateFlow<String?> = _selectedCategory



    // 供页面调用
    fun chooseCategory(cat: String?) { _selectedCategory.value = cat }

    fun addMeal(kcal: Int) {
        val idx = meals.size + 1
        meals.add(Meal(index = idx, name = "第${idx}餐", kcal = kcal, items = emptyList()))
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


    // 5) 今天第几餐：替换 getTodayMealIndex()
    fun getTodayMealIndex(userId: String, date: String, onResult: (Int) -> Unit) {
        if (userId.isBlank()) { onResult(1); return }
        viewModelScope.launch {
            val idx = try { remote.getMealIndex(userId, date) } catch (_: Exception) { 1 }
            onResult(idx)
        }
    }

    // 6) 保存餐次：替换 saveMeal()
    fun saveMeal(
        userId: String,
        date: String,
        mealIndex: Int,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (selectedItems.isEmpty()) return
        viewModelScope.launch {
            try {
                val foods = selectedItems.map { Remote.MealFoodUpload(it.name, it.grams, it.kcal) }
                remote.appendMeal(userId, date, mealIndex, foods)
                remote.markTaskCompleted(userId, date, Remote.TaskId.MEAL)
                clearAll()
                onComplete()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // 7) 加载当日汇总：替换 loadDataByDate()
    fun loadDataByDate(date: LocalDate) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val d = remote.loadDay(userId, date.toString())

                // 清空旧数据
                meals.clear()

                // 解析每餐
                val parsed = d.meals.map { mealMap ->
                    val foods = mealMap["foods"] as? List<Map<String, Any>> ?: emptyList()

                    val items = foods.map { f ->
                        val name  = (f["name"]  as? String).orEmpty()
                        val grams = (f["grams"] as? Number)?.toDouble() ?: 0.0
                        val kcal  = (f["kcal"]  as? Number)?.toInt() ?: 0
                        MealItem(name = name, grams = grams, kcal = kcal)
                    }

                    val idx = (mealMap["mealIndex"] as? Number)?.toInt()
                        ?: mealMap["mealIndex"]?.toString()?.toIntOrNull()
                        ?: (meals.size + 1)

                    val kcalSum = items.sumOf { it.kcal }

                    Meal(
                        index = idx,
                        name  = "第${idx}餐",
                        kcal  = kcalSum,
                        items = items
                    )
                }.sortedBy { it.index }

                meals.addAll(parsed)

                // 汇总
                totalIntakeKcal.value = d.totalCalories
                totalBurnKcal.value   = d.totalBurn

                // 解析 exercises（来自 Remote.loadDay 的 DayData.exercises）
                exercises.clear()
                d.exercises.forEachIndexed { idx, ex ->
                    val name    = (ex["name"]    as? String).orEmpty()
                    val minutes = (ex["minutes"] as? Number)?.toInt() ?: 0
                    val kcal    = (ex["kcal"]    as? Number)?.toInt() ?: 0
                    exercises.add(
                        ExerciseEntry(
                            index = idx + 1,
                            name = name,
                            minutes = minutes,
                            kcal = kcal
                        )
                    )
                }
                totalBurnKcal.value = d.totalBurn

            } catch (_: Exception) {
                meals.clear()
                totalIntakeKcal.value = 0
                totalBurnKcal.value   = 0
            }
        }
    }

    // FoodViewModel.kt
    fun deleteMealItem(date: LocalDate, mealIndex: Int, itemIndex: Int, onError: (Throwable)->Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                remote.deleteMealItem(uid, date.toString(), mealIndex, itemIndex)
                loadDataByDate(date)            // 已有
            } catch (e: Exception) { onError(e) }
        }
    }

    fun deleteExerciseItem(date: LocalDate, itemIndex: Int, onError: (Throwable)->Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                remote.deleteExerciseAt(uid, date.toString(), itemIndex)
                loadDataByDate(date)            // 已有
            } catch (e: Exception) { onError(e) }
        }
    }


    // 默认窗口：最近 60 天（按需改）
    private val trendWindowDays = 60
    private val _trendRange = MutableStateFlow(
        java.time.LocalDate.now().minusDays(trendWindowDays.toLong() - 1) to java.time.LocalDate.now()
    )
    val trendRange = _trendRange.asStateFlow()

    fun setTrendRange(daysBack: Int = trendWindowDays, endDate: java.time.LocalDate = java.time.LocalDate.now()) {
        _trendRange.value = endDate.minusDays(daysBack.toLong() - 1) to endDate
    }

    private val uid get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val hasUser get() = uid.isNotBlank()

    // 订阅：某个日期区间内的每日总摄入/总消耗
    private val trendFlow: StateFlow<List<Remote.DailyEnergy>> =
        trendRange.flatMapLatest { (start, end) ->
            if (!hasUser) flowOf(emptyList())
            else remote.observeRecordsRange(uid, start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    data class TrendSeries(
        val days: List<LocalDate>,
        val intake: List<Int>,
        val burn: List<Int>
    )
    // 订阅结果 + 窗口  =>  连续日期轴（缺失补0）
    private val trendSeries: StateFlow<TrendSeries> =
        combine(trendRange, trendFlow) { (start, end), list ->
            val byDate = list.associateBy { it.date }  // key: "YYYY-MM-DD"
            val days   = ArrayList<LocalDate>()
            val intake = ArrayList<Int>()
            val burn   = ArrayList<Int>()

            var d = start
            while (!d.isAfter(end)) {
                val key = d.toString()
                val e   = byDate[key]
                days.add(d)
                intake.add(e?.totalCalories ?: 0)
                burn.add(e?.totalBurn ?: 0)
                d = d.plusDays(1)
            }
            TrendSeries(days, intake, burn)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            TrendSeries(emptyList(), emptyList(), emptyList()))

    // 若你页面已用三个 StateFlow，就继续暴露三条（从 series 拆出来）
    val trendDays: StateFlow<List<LocalDate>> =
        trendSeries.map { it.days }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendIntake: StateFlow<List<Int>> =
        trendSeries.map { it.intake }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendBurn: StateFlow<List<Int>> =
        trendSeries.map { it.burn }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}