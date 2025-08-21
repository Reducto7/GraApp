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

class BodyMeasureViewModel(app: Application) : AndroidViewModel(app) {
    private val dao: BodyMeasureDao = AppDatabase.getInstance(app).bodyMeasureDao()
    private val repo = BodyMeasureRepository.create(app)

    // 当前图表选择的维度
    private val _selectedType = MutableStateFlow(BodyType.WAIST)
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

    fun addRecord(type: BodyType, value: Double, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            dao.insert(
                BodyMeasureEntity(
                    type = type.key,
                    date = date.toString(),
                    value = value,
                    unit = type.unit
                )
            )
        }
    }

    fun updateRecord(type: BodyType, id: String, newValue: Double, newDate: LocalDate) {
        viewModelScope.launch {
            try {
                repo.updateMeasure(type, id, newValue, newDate.toString())
                // 成功后，repo 内部应触发 Flow/StateFlow 刷新；或在这里手动 refresh()
            } catch (e: Exception) {
                Log.e("Measure", "update failed", e)
            }
        }
    }

    fun deleteRecord(type: BodyType, id: String) {
        viewModelScope.launch {
            try {
                repo.deleteMeasure(type, id)
            } catch (e: Exception) {
                Log.e("Measure", "delete failed", e)
            }
        }
    }
}
