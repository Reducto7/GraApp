package com.example.gra.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gra.ui.data.Remote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TasksViewModel : ViewModel() {
    private val remote = Remote.create()

    private val _tasks = MutableStateFlow<List<Remote.TaskState>>(emptyList())
    val tasks: StateFlow<List<Remote.TaskState>> = _tasks.asStateFlow()

    // 新增：每个任务的领取加载态
    private val _claiming = MutableStateFlow<Set<String>>(emptySet())
    val claiming: StateFlow<Set<String>> = _claiming.asStateFlow()

    fun start(uid: String, date: String) {
        if (uid.isBlank()) return
        // 监听任务列表
        remote.observeDailyTasks(uid, date)
            .onEach { _tasks.value = it }
            .launchIn(viewModelScope)
        // 兜底刷新一次完成状态
        viewModelScope.launch { remote.refreshDailyCompletion(uid, date) }
    }

    fun claim(
        uid: String,
        date: String,
        id: Remote.TaskId,
        onReward: (Int) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (uid.isBlank()) return
        if (_claiming.value.contains(id.id)) return  // 防重复点击

        _claiming.value = _claiming.value + id.id
        viewModelScope.launch {
            try {
                val r = remote.claimTask(uid, date, id)
                onReward(r)
            } catch (e: Exception) {
                onError(e)
            } finally {
                _claiming.value = _claiming.value - id.id
            }
        }
    }

    fun markLoginDone(uid: String, date: String) {
        if (uid.isBlank()) return
        viewModelScope.launch { remote.markTaskCompleted(uid, date, Remote.TaskId.LOGIN) }
    }
}
