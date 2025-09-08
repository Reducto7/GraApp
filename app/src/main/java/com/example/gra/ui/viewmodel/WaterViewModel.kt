package com.example.gra.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gra.ui.data.Remote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

data class WaterUiState(
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val goalMl: Int = 2000,
    val totalMl: Int = 0,
    val progress: Float = 0f,
    val entries: List<Pair<Int, Long>> = emptyList(),
    val signedIn: Boolean = true
)


class WaterViewModel : ViewModel() {

    // 🔑 直接在内部获取 Remote & 当前用户 uid
    private val remote = Remote.create()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val goalFlow = MutableStateFlow(0)

    private val selectedDate = MutableStateFlow(LocalDate.now())

    // ⬇️ 根据 selectedDate 订阅对应日期的数据
    private val dayFlow =
        selectedDate.flatMapLatest { d ->
            remote.observeWaterDay(uid, d.toString())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ui: StateFlow<WaterUiState> =
        combine(selectedDate, dayFlow, goalFlow) { selected, day, goal ->
            val dateStr = selected.toString()               // ✅ 永远显示“选中的日期”
            val g = if (goal > 0) goal else 2000
            val total = day?.totalMl ?: 0
            val prog = if (g > 0) (total / g.toFloat()).coerceIn(0f, 1f) else 0f
            val entries = day?.entries.orEmpty().map { it.ml to it.ts.toDate().time }
            WaterUiState(dateStr, g, total, prog, entries)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, WaterUiState())

    fun loadGoal() = viewModelScope.launch {
        goalFlow.value = remote.getWaterGoal(uid)
    }

    fun setGoal(goal: Int) = viewModelScope.launch {
        remote.setWaterGoal(uid, goal.coerceAtLeast(0))
        goalFlow.value = goal.coerceAtLeast(0)
    }

    // ✅ 切换当前记录日期
    fun setRecordDate(date: LocalDate) { selectedDate.value = date }

    // ✅ 按“选中日期”添加
    fun addWater(ml: Int) = viewModelScope.launch {
        val dateStr = selectedDate.value.toString()
        remote.addWater(uid, date = dateStr, ml = ml.coerceAtLeast(0))
    }

    // ✅ 按“选中日期”删除（不再从 ts 推回日期）
    fun deleteEntry(ts: Long) = viewModelScope.launch {
        val dateStr = selectedDate.value.toString()
        remote.deleteWater(uid, ts, dateStr)
    }

}
