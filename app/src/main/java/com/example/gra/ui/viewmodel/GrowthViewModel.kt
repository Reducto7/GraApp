package com.example.gra.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gra.ui.TREE_SEGMENTS
import com.example.gra.ui.data.Remote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GrowthViewModel : ViewModel() {
    private val remote = Remote.create()

    // 原来：private val _tree = MutableStateFlow(Remote.TreeState())
    private val _tree = MutableStateFlow(Remote.TreeState(level = 0, fed = 0, pending = 0))

    val tree: StateFlow<Remote.TreeState> = _tree.asStateFlow()

    fun start(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch { remote.initTreeIfAbsent(uid) }
        remote.observeTree(uid)
            .onEach { _tree.value = it }
            .launchIn(viewModelScope)
    }

    fun feedAll(uid: String, onDone: (Remote.FeedResult) -> Unit = {}) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val r = remote.feedAll(uid)
            onDone(r)
        }
    }

    class TreeStageViewModel : ViewModel() {
        private val _stageIndex = MutableStateFlow(-1) // 等待真实 level
        private val _playing    = MutableStateFlow(false)
        private val _manualOnce = MutableStateFlow(false) // “成长”按钮单次标记

        val stageIndexFlow: StateFlow<Int> = _stageIndex
        val playingFlow: StateFlow<Boolean> = _playing
        val manualFlow: StateFlow<Boolean> = _manualOnce

        val stageIndex get() = _stageIndex.value
        val playing    get() = _playing.value

        /** 播放从 startStage -> startStage+1 的动画（不改真实 level 的映射，专用于动画片段） */
        fun playFrom(startStage: Int) {
            if (_playing.value) return
            val clamped = startStage.coerceIn(0, TREE_SEGMENTS.lastIndex)
            _stageIndex.value = clamped         // 明确本次片段的“起点阶段”
            if (_stageIndex.value >= TREE_SEGMENTS.lastIndex) return
            _playing.value = true
        }

        /** 从真实 level 同步静态阶段；播放中不覆盖，避免回跳旧阶段 */
        fun setStageFromLevel(level: Int) {
            if (level < 0) return
            if (_playing.value) return          // ✅ 播放时忽略后端回流，防止闪回旧阶段
            _stageIndex.value = level.coerceIn(0, TREE_SEGMENTS.lastIndex)
        }

        fun markManualOnce() { _manualOnce.value = true }
        fun clearManual()    { _manualOnce.value = false }

        fun onOneSegmentFinished() {
            _playing.value = false
        }

        fun reset() {
            if (_playing.value) return
            _stageIndex.value = 0
            _playing.value = false
            _manualOnce.value = false
        }
    }

    fun upgrade(uid: String, onDone: (Boolean) -> Unit = {}) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val ok = remote.upgrade(uid)
            onDone(ok)
        }
    }

    fun forceLevelUp(uid: String, onDone: (Boolean) -> Unit = {}) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val ok = remote.forceLevelUp(uid)
            onDone(ok)
        }
    }

    fun resetLevel0(uid: String, onDone: (Boolean) -> Unit = {}) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val ok = remote.resetLevel0(uid)
            onDone(ok)
        }
    }

    fun markWaterDone(uid: String, date: String, onDone: () -> Unit = {}) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            remote.markTaskCompleted(uid, date, Remote.TaskId.WATER)
            onDone()
        }
    }

}
