package com.example.gra.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gra.ui.data.Remote
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class FriendsViewModel : ViewModel() {
    private val remote = Remote.create()

    // 新增：每个好友的 Profile 与 Tree
    private val _profiles = MutableStateFlow<Map<String, Remote.UserProfile?>>(emptyMap())
    val profiles: StateFlow<Map<String, Remote.UserProfile?>> = _profiles

    private val _trees = MutableStateFlow<Map<String, Remote.TreePublic?>>(emptyMap())
    val trees: StateFlow<Map<String, Remote.TreePublic?>> = _trees

    private val profileJobs = mutableMapOf<String, Job>()
    private val treeJobs = mutableMapOf<String, Job>()

    private val _friends = MutableStateFlow<List<Remote.FriendInfo>>(emptyList())
    val friends: StateFlow<List<Remote.FriendInfo>> = _friends

    private val _requests = MutableStateFlow<List<Remote.FriendRequest>>(emptyList())
    val requests: StateFlow<List<Remote.FriendRequest>> = _requests

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy


    // 删除模式（控制 UI）
    private val _deleteMode = MutableStateFlow(false)
    val deleteMode: StateFlow<Boolean> = _deleteMode
    fun toggleDeleteMode() { _deleteMode.value = !_deleteMode.value }

    fun start(uid: String) {
        if (uid.isBlank()) return
        // 监听好友列表
        remote.observeFriends(uid).onEach { list ->
            _friends.value = list
            // 维护子监听
            val set = list.map { it.uid }.toSet()
            attachProfileAndTreeWatchers(set)
        }.launchIn(viewModelScope)
        remote.observeFriendRequests(uid).onEach { _requests.value = it }.launchIn(viewModelScope)
    }

    private fun attachProfileAndTreeWatchers(uids: Set<String>) {
        // 取消已移除好友的子监听
        profileJobs.keys.filter { it !in uids }.forEach { k ->
            profileJobs.remove(k)?.cancel()
            _profiles.update { it - k }
        }
        treeJobs.keys.filter { it !in uids }.forEach { k ->
            treeJobs.remove(k)?.cancel()
            _trees.update { it - k }
        }
        // 为新增好友创建子监听
        uids.forEach { fid ->
            if (profileJobs[fid] == null) {
                profileJobs[fid] = remote.observeUserProfile(fid)
                    .onEach { p ->
                        _profiles.update { it + (fid to p) }
                        Log.d("Friends", "profile[$fid] -> ${p?.uniqueId}")
                    }
                    .launchIn(viewModelScope)
            }
            if (treeJobs[fid] == null) {
                treeJobs[fid] = remote.observeTreePublic(fid)
                    .onEach { t ->
                        _trees.update { it + (fid to t) }
                        Log.d("Friends", "tree[$fid] -> L${t?.level} fed=${t?.fed}")
                    }
                    .launchIn(viewModelScope)
            }
        }
    }


    fun send(uid: String, targetUniqueId: String, onMsg: (String)->Unit) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            runCatching { remote.sendFriendRequest(uid, targetUniqueId) }
                .onSuccess { onMsg(if (it) "已发送申请" else "发送失败或已是好友") }
                .onFailure { onMsg("发送失败：${it.localizedMessage}") }
            _busy.value = false
        }
    }

    fun accept(uid: String, fromUid: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { remote.respondFriendRequest(uid, fromUid, true) }
                .onSuccess { onMsg("已同意") }
                .onFailure { onMsg("操作失败：${it.localizedMessage}") }
            _busy.value = false
        }
    }
    fun reject(uid: String, fromUid: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { remote.respondFriendRequest(uid, fromUid, false) }
                .onSuccess { onMsg("已拒绝") }
                .onFailure { onMsg("操作失败：${it.localizedMessage}") }
            _busy.value = false
        }
    }
    fun remove(uid: String, otherUid: String, onMsg: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val ok = remote.removeFriend(uid, otherUid)
                if (ok) {
                    onMsg("已删除好友")
                } else {
                    onMsg("删除失败：请稍后重试")
                }
            } catch (e: Exception) {
                onMsg("删除失败：${e.localizedMessage ?: e::class.simpleName}")
            } finally {
                _busy.value = false
            }
        }
    }


    // ===== 互动功能 =====
    fun gift(uid: String, friendUid: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val ok = remote.giftToFriend(uid, friendUid)
                if (ok) {
                    onMsg("已赠送 5 点")
                    // ✅ 赠送任务：gift_once
                    runCatching {
                        remote.markTaskCompleted(uid, LocalDate.now().toString(), Remote.TaskId.GIFT_ONCE)
                    }
                } else {
                    onMsg("今天已赠送过该好友")
                }
            } catch (e: Exception) {
                onMsg("赠送失败：${e.localizedMessage}")
            } finally {
                _busy.value = false
            }
        }
    }

    fun claim(uid: String, friendUid: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val got = remote.claimFromFriend(uid, friendUid)
                if (got > 0) {
                    onMsg("已领取 $got 点")
                    // ✅ 领取任务：claim_once
                    runCatching {
                        remote.markTaskCompleted(uid, LocalDate.now().toString(), Remote.TaskId.CLAIM_ONCE)
                    }
                } else {
                    onMsg("今日额度已满或无可领取")
                }
            } catch (e: Exception) {
                onMsg("领取失败：${e.localizedMessage}")
            } finally {
                _busy.value = false
            }
        }
    }

    fun giftAll(uid: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val (ok, fail) = remote.giftAll(uid, _friends.value.map { it.uid })
                onMsg("一键赠送完成：成功 $ok，失败 $fail")
            } catch (e: Exception) {
                onMsg("一键赠送失败：${e.localizedMessage}")
            } finally {
                _busy.value = false
            }
        }
    }

    fun claimAll(uid: String, onMsg: (String)->Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val got = remote.claimAll(uid, _friends.value.map { it.uid })
                onMsg(if (got > 0) "一键领取：共获得 $got 点" else "今日额度已满或无可领取")
            } catch (e: Exception) {
                onMsg("一键领取失败：${e.localizedMessage}")
            } finally {
                _busy.value = false
            }
        }
    }
}
