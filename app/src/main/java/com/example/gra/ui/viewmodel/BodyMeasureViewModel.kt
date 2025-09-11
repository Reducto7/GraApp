package com.example.gra.ui.viewmodel

// BodyMeasureViewModel.kt

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gra.ui.data.AppDatabase
import com.example.gra.ui.data.BodyMeasureDao
import com.example.gra.ui.data.BodyMeasureEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.gra.ui.data.BodyMeasureRepository
import com.example.gra.ui.data.Remote
import com.google.firebase.auth.FirebaseAuth

enum class BodyType(val key: String, val label: String, val unit: String) {
    WEIGHT("weight", "体重", "kg"),
    WAIST("waist", "腰围", "cm"),
    CHEST("chest", "胸围", "cm"),
    HIP("hip", "臀围", "cm"),
    THIGH("thigh", "大腿围", "cm"),
    UPPER_ARM("upperarm", "大臂围", "cm")
}
// BodyMeasureViewModel.kt 顶部或伴生对象里
val BODY_DISPLAY_ORDER = listOf(
    BodyType.WEIGHT,      // 体重
    BodyType.WAIST,      // 腰围
    BodyType.THIGH,      // 大腿围
    BodyType.UPPER_ARM,  // 大臂围
    BodyType.HIP,        // 臀围
    BodyType.CHEST,      // 胸围
)

enum class Sex { Male, Female }

class BodyMeasureViewModel(app: Application) : AndroidViewModel(app) {
    // 2) 成员
    private val remote = Remote.create()

    // 3) 新增一个便捷取 uid
    private fun uidOrNull(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private val dao: BodyMeasureDao = AppDatabase.getInstance(app).bodyMeasureDao()
    private val repo = BodyMeasureRepository.create(app)

    // 当前图表选择的维度
    private val _selectedType = MutableStateFlow(BODY_DISPLAY_ORDER.first())
    val selectedType: StateFlow<BodyType> = _selectedType

    fun select(type: BodyType) { _selectedType.value = type }

    // 各维度的最新值
    val latestMap: StateFlow<Map<BodyType, BodyMeasureEntity?>> =
        combine(BodyType.values().map { t -> dao.latest(t.key) }) { arr ->
            BodyType.values().associateWith { idx -> arr[BodyType.values().indexOf(idx)] }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // 当前维度的历史
    val history: StateFlow<List<BodyMeasureEntity>> =
        _selectedType.flatMapLatest { t -> dao.history(t.key) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 4) 新增/更新/删除时，同时同步到 Firestore（按类型归档）
    fun addRecord(type: BodyType, value: Double, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            // 本地
            dao.insert(BodyMeasureEntity(type = type.key, date = date.toString(), value = value, unit = type.unit))
            // 远端
            uidOrNull()?.let { uid ->
                try {
                    remote.upsertBodyMetric(uid, type.key, date.toString(), value, type.unit)
                    remote.markTaskCompleted(uid, date.toString(), Remote.TaskId.BODY)
                } catch (_: Exception) {}
            }
        }
    }
    fun updateRecord(type: BodyType, id: String, newValue: Double, newDate: LocalDate) {
        viewModelScope.launch {
            try {
                // 先查旧记录，便于远端删除旧 doc（若日期被改）
                val old = dao.findById(id)
                // 本地更新
                repo.updateMeasure(type, id, newValue, newDate.toString())
                // 远端处理
                uidOrNull()?.let { uid ->
                    try {
                        if (old != null && old.date != newDate.toString()) {
                            // 日期改变：删旧建新
                            remote.deleteBodyMetric(uid, type.key, old.date)
                        }
                        remote.upsertBodyMetric(uid, type.key, newDate.toString(), newValue, type.unit)
                        remote.markTaskCompleted(uid, newDate.toString(), Remote.TaskId.BODY)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e("Measure", "update failed", e)
            }
        }
    }

    fun deleteRecord(type: BodyType, id: String) {
        viewModelScope.launch {
            try {
                val old = dao.findById(id)
                repo.deleteMeasure(type, id)
                uidOrNull()?.let { uid ->
                    if (old != null) {
                        try { remote.deleteBodyMetric(uid, type.key, old.date) } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.e("Measure", "delete failed", e)
            }
        }
    }

    // ---------- 健康档案 UI State ----------
    data class HealthUi(
        val sex: String = "male",
        val heightCm: Double = 0.0,
        val age: Int = 0,
        val weightKg: Double = 0.0,
        val bmr: Int = 0,
        val tdee: Int = 0,
        val recoMaintain: Int = 0,
        val recoCut: Int = 0,

        // ★ 新增（计划 & 主题）
        val planIntakeKcalPerDay: Int? = null,
        val planIntakeMode: String? = null,
        val planBurnKcalPerDay: Int? = null,
        val planWorkoutMode: String? = null,
        val themePalette: String? = null
    )


    private val _health = MutableStateFlow<HealthUi?>(null)
    val health: StateFlow<HealthUi?> = _health

    init {
        // 订阅远端 health 文档
        viewModelScope.launch {
            uidOrNull()?.let { uid ->
                remote.observeHealthProfile(uid).collect { hp ->
                    _health.value = hp?.let {
                        HealthUi(
                            sex = it.sex ?: "male",
                            heightCm = it.heightCm ?: 0.0,
                            age = it.age ?: 0,
                            weightKg = it.weightKg ?: 0.0,
                            bmr = it.bmr ?: 0,
                            tdee = it.tdee ?: 0,
                            recoMaintain = it.recoMaintain ?: 0,
                            recoCut = it.recoCut ?: 0,

                            // ★ 新增
                            planIntakeKcalPerDay = it.planIntakeKcalPerDay,
                            planIntakeMode       = it.planIntakeMode,
                            planBurnKcalPerDay   = it.planBurnKcalPerDay,
                            planWorkoutMode      = it.planWorkoutMode,
                            themePalette         = it.themePalette
                        )
                    }

                }
            }
        }
    }

    // ---------- 计算推荐（Mifflin–St Jeor） ----------
    private fun computeTargets(
        sex: String,
        heightCm: Double,
        weightKg: Double,
        age: Int,
        activityFactor: Double = 1.4  // 轻中等活动；你可根据实际改成用户可选
    ): Triple<Int, Int, Pair<Int, Int>> {
        val bmr = ((10 * weightKg) + (6.25 * heightCm) - (5 * age) + if (sex == "female") -161 else 5).toInt()
        val tdee = (bmr * activityFactor).toInt()
        val maintain = tdee
        val cut = (tdee * 0.85).toInt() // 示例：维持 -15%
        return Triple(bmr, tdee, maintain to cut)
    }

    // ---------- 保存健康档案（Onboarding / Mine 都调它） ----------
    fun saveHealthProfile(sex: String, heightCm: Double, weightKg: Double, age: Int) {
        viewModelScope.launch {
            val (bmr, tdee, mc) = computeTargets(sex, heightCm, weightKg, age)
            val maintain = mc.first
            val cut = mc.second
            uidOrNull()?.let { uid ->
                try {
                    remote.setHealthProfile(
                        uid,
                        Remote.HealthProfile(
                            sex = sex,
                            heightCm = heightCm,
                            age = age,
                            weightKg = weightKg,
                            bmr = bmr,
                            tdee = tdee,
                            recoMaintain = maintain,
                            recoCut = cut
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Health", "save failed", e)
                }
            }
        }
    }

    /** 只改某一两个字段用（编辑卡片方便） */
    fun updateHealth(fields: Map<String, Any?>) {
        viewModelScope.launch {
            uidOrNull()?.let { uid ->
                try { remote.updateHealthFields(uid, fields) } catch (_: Exception) {}
            }
        }
    }

}
