package com.example.gra.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gra.ui.data.Remote
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupsViewModel : ViewModel() {
    private val remote = Remote.create()

    private val _myRooms = MutableStateFlow<List<Remote.RoomInfo>>(emptyList())
    val myRooms: StateFlow<List<Remote.RoomInfo>> = _myRooms

    private val _searchResults = MutableStateFlow<List<Remote.RoomInfo>>(emptyList())
    val searchResults: StateFlow<List<Remote.RoomInfo>> = _searchResults

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun start(uid: String) {
        if (uid.isBlank()) return
        remote.observeMyRooms(uid).onEach { _myRooms.value = it }.launchIn(viewModelScope)
    }

    fun create(uid: String, name: String, onMsg: (String)->Unit) {
        if (uid.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            try {
                val id = remote.createRoom(uid, name.trim())
                onMsg("已创建：$id")
            } catch (e: Exception) {
                onMsg("创建失败：${e.localizedMessage}")
            } finally { _busy.value = false }
        }
    }

    fun search(q: String) {
        viewModelScope.launch {
            try {
                _searchResults.value = remote.findRoomByIdOrName(q)
            } catch (_: Exception) { _searchResults.value = emptyList() }
        }
    }

    fun join(uid: String, roomId: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val ok = remote.joinRoom(uid, roomId)
                onMsg(if (ok) "已加入 $roomId" else "房间不存在")
            } catch (e: Exception) {
                onMsg("加入失败：${e.localizedMessage}")
            } finally { _busy.value = false }
        }
    }

    fun leave(uid: String, roomId: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val ok = remote.leaveRoom(uid, roomId)
                onMsg(if (ok) "已退出 $roomId" else "退出失败")
            } catch (e: Exception) {
                onMsg(e.localizedMessage ?: "退出失败")
            } finally { _busy.value = false }
        }
    }

    fun dissolve(uid: String, roomId: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val ok = remote.dissolveRoom(uid, roomId)
                onMsg(if (ok) "已解散 $roomId" else "解散失败")
            } catch (e: Exception) {
                onMsg(e.localizedMessage ?: "解散失败")
            } finally { _busy.value = false }
        }
    }

    // GroupsViewModel.kt 追加字段与逻辑

    private val _selectedRoomId = MutableStateFlow<String?>(null)
    val selectedRoomId: StateFlow<String?> = _selectedRoomId

    private val _members = MutableStateFlow<List<Remote.RoomMember>>(emptyList())
    val members: StateFlow<List<Remote.RoomMember>> = _members

    private val _profiles = MutableStateFlow<Map<String, Remote.UserProfile?>>(emptyMap())
    val profiles: StateFlow<Map<String, Remote.UserProfile?>> = _profiles

    private val _trees = MutableStateFlow<Map<String, Remote.TreePublic?>>(emptyMap())
    val trees: StateFlow<Map<String, Remote.TreePublic?>> = _trees

    private val profileJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val treeJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // VM里新增

    private val _selectedRoom = MutableStateFlow<Remote.RoomInfo?>(null)
    val selectedRoom: StateFlow<Remote.RoomInfo?> = _selectedRoom

    fun openRoom(roomId: String) {
        _selectedRoomId.value = roomId
        // 监听 rooms/{roomId}
        remote.observeRoom(roomId)
            .onEach { _selectedRoom.value = it }
            .launchIn(viewModelScope)

        // 监听成员
        remote.observeRoomMembers(roomId)
            .onEach { list ->
                _members.value = list
                attachProfileAndTreeWatchers(list.map { it.uid }.toSet())
            }
            .launchIn(viewModelScope)
    }

    fun closeRoom() {
        _selectedRoomId.value = null
        // 清理
        profileJobs.values.forEach { it.cancel() }
        treeJobs.values.forEach { it.cancel() }
        profileJobs.clear(); treeJobs.clear()
        _profiles.value = emptyMap()
        _trees.value = emptyMap()
        _members.value = emptyList()
    }

    private fun attachProfileAndTreeWatchers(uids: Set<String>) {
        // 移除不再需要的
        profileJobs.keys.filter { it !in uids }.forEach { k -> profileJobs.remove(k)?.cancel(); _profiles.update { it - k } }
        treeJobs.keys.filter { it !in uids }.forEach { k -> treeJobs.remove(k)?.cancel(); _trees.update { it - k } }

        // 新增
        uids.forEach { id ->
            if (profileJobs[id] == null) {
                profileJobs[id] = remote.observeUserProfile(id)
                    .onEach { p -> _profiles.update { it + (id to p) } }
                    .launchIn(viewModelScope)
            }
            if (treeJobs[id] == null) {
                treeJobs[id] = remote.observeTreePublic(id)
                    .onEach { t -> _trees.update { it + (id to t) } }
                    .launchIn(viewModelScope)
            }
        }
    }

    /** 群内打卡（本人） */
    // GroupsViewModel.kt 中的打卡函数
    fun checkin(uid: String, onMsg: (String)->Unit) {
        val rid = _selectedRoomId.value ?: return
        viewModelScope.launch {
            _busy.value = true
            try {
                val fresh = remote.checkinRoom(uid, rid)
                if (fresh) {
                    // 今日日期（与 TasksPage 使用 LocalDate 的字符串一致）
                    val today = java.time.LocalDate.now().toString()
                    // ✅ 群内打卡任务：完成一次
                    remote.markTaskCompleted(uid, today, Remote.TaskId.GROUP_CHECKIN)
                    onMsg("打卡成功")
                } else {
                    onMsg("今天已打过卡")
                }
            } catch (e: Exception) {
                onMsg("打卡失败：${e.localizedMessage}")
            } finally {
                _busy.value = false
            }
        }
    }


    /** 今日统计 */
    val todayCheckedCount: StateFlow<Int> =
        combine(_members) { mems ->
            val today = java.util.Calendar.getInstance().let { c ->
                c.get(java.util.Calendar.YEAR) * 10000 +
                        (c.get(java.util.Calendar.MONTH) + 1) * 100 +
                        c.get(java.util.Calendar.DAY_OF_MONTH)
            }
            mems.firstOrNull()?.let {
                // 计算
                _members.value.count { it.checkDate == today }
            } ?: 0
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

}
