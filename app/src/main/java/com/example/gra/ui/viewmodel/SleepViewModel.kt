package com.example.gra.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gra.ui.data.Remote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*

// 图表柱：以“入睡日”为桶，中线是 24:00，上=醒，下=睡
data class NightBar(
    val bedDate: LocalDate,
    val bedMinutes: Int,   // 入睡时刻（当天分钟 0..1440）
    val wakeMinutes: Int   // 次日醒来时刻（分钟 0..1440）
)

data class SleepFormState(
    val startDate: LocalDate = LocalDate.now().minusDays(1),
    val startTime: LocalTime = LocalTime.of(23, 30),
    val endDate: LocalDate = LocalDate.now(),
    val endTime: LocalTime = LocalTime.of(7, 0),
    val isNap: Boolean = false,
    val note: String = "",
    val durationText: String = "0h 0m",
    val isValid: Boolean = false
)

data class SleepUiState(
    val bars: List<NightBar> = emptyList(),                  // 柱图数据（晚->早）
    val daily: List<Remote.SleepDaily> = emptyList(),        // 折线图数据（每日总分钟）
    val rangeStart: LocalDate = LocalDate.now().minusDays(179),
    val rangeEnd: LocalDate = LocalDate.now(),
    val form: SleepFormState = SleepFormState(),
    val signedIn: Boolean = true
)

class SleepViewModel : ViewModel() {

    private val TAG = "SleepVM"

    val saving = MutableStateFlow(false)

    private val remote = Remote.create()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val hasUser get() = uid.isNotBlank()
    private val zoneId = ZoneId.systemDefault()

    // ✅ 固定窗口：最近 180 天
    private val windowDays = 180
    private val _range = MutableStateFlow(
        LocalDate.now().minusDays(windowDays.toLong() - 1) to LocalDate.now()
    )
    val range = _range.asStateFlow()
    private val _form = MutableStateFlow(SleepFormState().recalcDuration())

    // --- sessions：构柱图 ---
    private val sessionsFlow: StateFlow<List<Remote.SleepSession>> =
        range.flatMapLatest { (startDate, endDate) ->
            if (!hasUser) flowOf(emptyList()) else {
                val from = startDate.atStartOfDay(zoneId).toInstant()
                val to   = endDate.plusDays(1).atStartOfDay(zoneId).toInstant()
                remote.observeSleepSessions(uid, from, to)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 将 sessions 聚合成每条独立的睡眠记录
    private val barsFlow: StateFlow<List<NightBar>> =
        sessionsFlow.map { sessions ->
            sessions.map { session ->
                val start = session.startTs.toDate().toInstant()
                val end = session.endTs.toDate().toInstant()

                // 将每条会话数据转换成柱状图的数据（NightBar）
                val bedMinutes = start.atZone(zoneId).toLocalTime().let { it.hour * 60 + it.minute }
                val wakeMinutes = end.atZone(zoneId).toLocalTime().let { it.hour * 60 + it.minute }
                val bedDate = start.atZone(zoneId).toLocalDate()

                // 如果凌晨入睡，则底端时间为前一天
                val actualBedDate = if (bedMinutes < 720) bedDate.minusDays(1) else bedDate

                NightBar(actualBedDate, bedMinutes, wakeMinutes)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- daily：给折线图用（每日总分钟） ---
    private val dailyFlow: StateFlow<List<Remote.SleepDaily>> =
        range.flatMapLatest { (startDate, endDate) ->
            if (!hasUser) flowOf(emptyList())
            else remote.observeSleepDaily(uid, startDate, endDate)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ui: StateFlow<SleepUiState> =
        combine(range, barsFlow, dailyFlow, _form) { (s, e), bars, daily, form ->
            SleepUiState(
                bars = bars,
                daily = daily,
                rangeStart = s,
                rangeEnd = e,
                form = form,
                signedIn = hasUser
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SleepUiState(signedIn = hasUser))

    fun init() { /* 预留：需要时可在此加载目标等 */ }


    // ---- 表单联动 ----
    fun setStartDate(d: LocalDate) = updateForm { copy(startDate = d).recalcDuration() }
    fun setStartTime(t: LocalTime) = updateForm { copy(startTime = t).recalcDuration() }
    fun setEndDate(d: LocalDate)   = updateForm { copy(endDate = d).recalcDuration() }
    fun setEndTime(t: LocalTime)   = updateForm { copy(endTime = t).recalcDuration() }
    fun toggleNap()                = updateForm { copy(isNap = !isNap) }
    fun setNote(s: String)         = updateForm { copy(note = s.take(200)) }

    private fun updateForm(block: SleepFormState.() -> SleepFormState) {
        _form.value = _form.value.block()
    }

    private fun SleepFormState.recalcDuration(): SleepFormState {
        val start = tryInstant(startDate, startTime)
        val end   = tryInstant(endDate, endTime)
        val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(zoneId).toInstant() // 明天 00:00 作为“今天的上限”
        val valid = (start != null && end != null && end.isAfter(start) && end.isBefore(todayEnd.plusSeconds(1)))
        val text = if (start != null && end != null && end.isAfter(start)) {
            val min = Duration.between(start, end).toMinutes().toInt()
            "${min / 60}h ${min % 60}m"
        } else "0h 0m"
        return copy(durationText = text, isValid = valid)
    }

    private fun tryInstant(d: LocalDate, t: LocalTime): Instant? =
        try { ZonedDateTime.of(d, t, zoneId).toInstant() } catch (_: Exception) { null }

    // 一次性事件：用于提示和调试
    sealed class SleepEvent {
        object Saved : SleepEvent()
        data class Error(val msg: String) : SleepEvent()
    }
    val events = MutableSharedFlow<SleepEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun submit() = viewModelScope.launch {
        if (!hasUser) {
            Log.w(TAG, "submit: no user")
            events.tryEmit(SleepEvent.Error("未登录"))
            return@launch
        }
        val f = _form.value
        val start = tryInstant(f.startDate, f.startTime)
        val end   = tryInstant(f.endDate, f.endTime)

        Log.d(TAG, "submit: start=$start end=$end isNap=${f.isNap} note='${f.note}' valid=${f.isValid}")

        if (start == null || end == null) {
            events.tryEmit(SleepEvent.Error("时间无效"))
            return@launch
        }
        if (!end.isAfter(start)) {
            events.tryEmit(SleepEvent.Error("结束时间必须晚于开始时间"))
            return@launch
        }

        // 检查是否与已有记录时间重叠
        val overlappingRecord = sessions.value.find { s ->
            val existingStart = s.startTs.toDate().toInstant()
            val existingEnd = s.endTs.toDate().toInstant()

            // 判断是否重叠：新记录入睡时间或醒来时间是否和已有记录重叠
            (start.isAfter(existingStart) && start.isBefore(existingEnd)) ||
                    (end.isAfter(existingStart) && end.isBefore(existingEnd))
        }

        if (overlappingRecord != null) {
            events.tryEmit(SleepEvent.Error("有重叠的睡眠记录，无法添加"))  // 提示用户重叠记录
            return@launch
        }

        // 提交数据
        saving.value = true
        runCatching {
            remote.addSleepSession(
                uid = uid,
                start = start,
                end = end,
                isNap = f.isNap,
                note = f.note.takeIf { it.isNotBlank() },
                zoneId = zoneId
            )
        }.onSuccess {
            Log.d(TAG, "submit: success id=$it")
            events.tryEmit(SleepEvent.Saved)
            _form.value = SleepFormState().recalcDuration() // 重置回默认昨晚/今早
        }.onFailure { e ->
            Log.e(TAG, "submit: failure ${e.message}", e)
            events.tryEmit(SleepEvent.Error(e.localizedMessage ?: "保存失败"))
        }
        saving.value = false
    }

    // 供 UI 使用的历史数据（与图表同一时间范围）
    val sessions: StateFlow<List<Remote.SleepSession>> = sessionsFlow

    fun deleteSession(id: String) = viewModelScope.launch {
        if (!hasUser) {
            events.tryEmit(SleepEvent.Error("未登录"))
            return@launch
        }

        runCatching {
            remote.deleteSleepSession(uid, id, zoneId)  // ⬅️ 调远端删除
        }
            .onSuccess {
                // 成功通常无需手动刷新，sessionsFlow 会被监听自动更新
                // 如需提示：
                // events.tryEmit(SleepEvent.Message("已删除"))
            }
            .onFailure { e ->
                Log.e(TAG, "deleteSession failed: ${e.message}", e)
                events.tryEmit(SleepEvent.Error(e.message ?: "删除失败"))
            }
    }

}
