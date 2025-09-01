package com.example.gra.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gra.ui.data.Remote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class WaterUiState(
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val goalMl: Int = 2000,
    val totalMl: Int = 0,
    val progress: Float = 0f, // 0..1
    val entries: List<Pair<Int, Long>> = emptyList(), // (ml, epochMillis) desc
    val signedIn: Boolean = true  // ✅ 这个字段很重要
)


class WaterViewModel : ViewModel() {

    // 🔑 直接在内部获取 Remote & 当前用户 uid
    private val remote = Remote.create()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val goalFlow = MutableStateFlow(0)

    private val dayFlow =
        remote.observeWaterDay(uid).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ui: StateFlow<WaterUiState> =
        combine(dayFlow, goalFlow) { day, goal ->
            val date = day?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val g = if (goal > 0) goal else 2000
            val total = day?.totalMl ?: 0
            val prog = if (g > 0) (total / g.toFloat()).coerceIn(0f, 1f) else 0f
            val entries = day?.entries.orEmpty().map { it.ml to it.ts.toDate().time }
            WaterUiState(date, g, total, prog, entries)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, WaterUiState())

    fun loadGoal() = viewModelScope.launch {
        goalFlow.value = remote.getWaterGoal(uid)
    }

    fun setGoal(goal: Int) = viewModelScope.launch {
        remote.setWaterGoal(uid, goal.coerceAtLeast(0))
        goalFlow.value = goal.coerceAtLeast(0)
    }

    fun addWater(ml: Int) = viewModelScope.launch {
        remote.addWater(uid, ml = ml.coerceAtLeast(0))
    }

    fun deleteEntry(ts: Long) = viewModelScope.launch {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))
        remote.deleteWater(uid, ts, date)
    }

}
