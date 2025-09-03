package com.example.gra.ui.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Single entry-point for all Firestore reads/writes used by the app.
 *
 * Firestore layout (ENGLISH collection names only):
 *
 * users/{uid}/
 *   favorites/
 *     food/{foodId}
 *     exercise/{exerciseId}
 *   records/{date}
 *     - meals: [ { mealIndex: Int, foods: [ { name, grams, kcal } ] } ]
 *     - totalCalories: Int
 *     - exercises: [ { name, minutes, kcal } ]
 *     - totalBurn: Int
 *   body_metrics/{type}/records/{date}
 *     - { value: Double, unit: String, date: "YYYY-MM-DD", ts: serverTimestamp }
 *
 * Backward-compat for existing data paths used in ViewModels:
 *  - Food favorites were in  users/{uid}/favorites
 *  - Exercise favorites were in users/{uid}/exercise_favorites
 *
 * This Remote class reads both new and old paths, and writes to the new ones.
 */


class Remote(private val db: FirebaseFirestore = Firebase.firestore) {

    companion object {
        fun create(): Remote = Remote(Firebase.firestore)
    }

    private fun user(uid: String) = db.collection(COLL_USERS).document(uid)

    // ------------------------------
    // Favorites (Food & Exercise)
    // ------------------------------

    fun observeFoodFavorites(uid: String): Flow<Set<String>> {
        val newPath = user(uid).collection(COLL_FAVORITES).document(SUB_FOOD).collection(SUB_IDS)
        val oldPath = user(uid).collection(OLD_FOOD_FAV)
        return combine(listenIdSet(newPath), listenIdSet(oldPath)) { a, b -> a + b }
    }

    fun observeExerciseFavorites(uid: String): Flow<Set<String>> {
        val newPath = user(uid).collection(COLL_FAVORITES).document(SUB_EXERCISE).collection(SUB_IDS)
        val oldPath = user(uid).collection(OLD_EXERCISE_FAV)
        return combine(listenIdSet(newPath), listenIdSet(oldPath)) { a, b -> a + b }
    }

    suspend fun addFoodFavorite(uid: String, foodId: String, name: String? = null) {
        // Write ONLY to new path
        val ref = user(uid)
            .collection(COLL_FAVORITES).document(SUB_FOOD)
            .collection(SUB_IDS).document(foodId)
        ref.set(mapOf(
            "name" to name,
            "ts" to FieldValue.serverTimestamp()
        ), SetOptions.merge()).await()
    }

    suspend fun removeFoodFavorite(uid: String, foodId: String) {
        user(uid)
            .collection(COLL_FAVORITES).document(SUB_FOOD)
            .collection(SUB_IDS).document(foodId)
            .delete().await()
    }

    suspend fun addExerciseFavorite(uid: String, exerciseId: String, name: String? = null) {
        val ref = user(uid)
            .collection(COLL_FAVORITES).document(SUB_EXERCISE)
            .collection(SUB_IDS).document(exerciseId)
        ref.set(mapOf(
            "name" to name,
            "ts" to FieldValue.serverTimestamp()
        ), SetOptions.merge()).await()
    }

    suspend fun removeExerciseFavorite(uid: String, exerciseId: String) {
        user(uid)
            .collection(COLL_FAVORITES).document(SUB_EXERCISE)
            .collection(SUB_IDS).document(exerciseId)
            .delete().await()
    }

    // ------------------------------
    // Records (Meals & Exercises)
    // ------------------------------

    data class MealFoodUpload(val name: String, val grams: Double, val kcal: Int)
    data class ExerciseUpload(val name: String, val minutes: Int, val kcal: Int)

    suspend fun getMealIndex(uid: String, date: String): Int {
        val doc = user(uid).collection(COLL_RECORDS).document(date).get().await()
        val meals = doc.get("meals") as? List<*> ?: emptyList<Any>()
        return if (meals.isEmpty()) 1 else meals.size + 1
    }

    suspend fun appendMeal(uid: String, date: String, mealIndex: Int, foods: List<MealFoodUpload>) {
        val docRef = user(uid).collection(COLL_RECORDS).document(date)
        db.runTransaction { tr ->
            val snap = tr.get(docRef)
            // existing meals
            val meals = (snap.get("meals") as? ArrayList<Map<String, Any>>)?.toMutableList()
                ?: mutableListOf()

            val mealMap = mapOf(
                "mealIndex" to mealIndex,
                "foods" to foods.map { mapOf(
                    "name" to it.name,
                    "grams" to it.grams,
                    "kcal" to it.kcal,
                ) }
            )
            meals.add(mealMap)

            val newTotal = meals.sumOf { m ->
                val fs = m["foods"] as? List<Map<String, Any>> ?: emptyList()
                fs.sumOf { (it["kcal"] as? Number)?.toInt() ?: 0 }
            }

            tr.set(docRef, mapOf(
                "meals" to meals,
                "totalCalories" to newTotal
            ), SetOptions.merge())
            null
        }.await()
    }

    // Remote.kt  —— 加在 Records (Meals & Exercises) 区域里
    suspend fun deleteMealItem(
        uid: String,
        date: String,
        mealPos: Int,   // UI显示的“第X餐”，从1开始
        itemPos: Int    // 该餐里第几个条目，从0开始
    ) {
        val docRef = user(uid).collection(COLL_RECORDS).document(date)
        db.runTransaction { tr ->
            val snap = tr.get(docRef)

            // 取出 meals（List<Map>）
            val meals = (snap.get("meals") as? ArrayList<Map<String, Any?>>)
                ?.map { it.toMutableMap() }
                ?.toMutableList() ?: mutableListOf()

            val mIdx = mealPos - 1
            if (mIdx !in meals.indices) return@runTransaction null

            // 取出该餐 foods
            val foods = (meals[mIdx]["foods"] as? ArrayList<Map<String, Any?>>)
                ?.toMutableList() ?: mutableListOf()

            if (itemPos !in foods.indices) return@runTransaction null

            // 删除该项
            foods.removeAt(itemPos)

            if (foods.isEmpty()) {
                // 该餐已空 -> 移除该餐
                meals.removeAt(mIdx)
                // 重排 mealIndex 为 1..N
                meals.forEachIndexed { i, mealMap ->
                    mealMap["mealIndex"] = i + 1
                }
            } else {
                // 还有剩余 -> 写回 foods
                meals[mIdx]["foods"] = foods
            }

            // 重新汇总 totalCalories
            val newTotal = meals.sumOf { m ->
                val fs = (m["foods"] as? List<Map<String, Any?>>) ?: emptyList()
                fs.sumOf { (it["kcal"] as? Number)?.toInt() ?: 0 }
            }

            tr.set(
                docRef,
                mapOf(
                    "meals" to meals,
                    "totalCalories" to newTotal
                ),
                SetOptions.merge()
            )
            null
        }.await()
    }


    suspend fun appendExercises(uid: String, date: String, list: List<ExerciseUpload>) {
        val docRef = user(uid).collection(COLL_RECORDS).document(date)
        db.runTransaction { tr ->
            val snap = tr.get(docRef)
            val oldBurn = snap.getLong("totalBurn")?.toInt() ?: 0
            val add = list.sumOf { it.kcal }

            val arrayToAdd = list.map {
                mapOf("name" to it.name, "minutes" to it.minutes, "kcal" to it.kcal)
            }.toTypedArray()

            tr.set(docRef, mapOf(
                "totalBurn" to (oldBurn + add)
            ), SetOptions.merge())

            tr.update(docRef, mapOf(
                "exercises" to FieldValue.arrayUnion(*arrayToAdd)
            ))
            null
        }.await()
    }

    // Remote.kt —— 和 appendExercises 同一区域
    suspend fun deleteExerciseAt(uid: String, date: String, itemPos: Int) {
        val docRef = user(uid).collection(COLL_RECORDS).document(date)
        db.runTransaction { tr ->
            val snap = tr.get(docRef)

            val list = (snap.get("exercises") as? ArrayList<Map<String, Any?>>)
                ?.toMutableList() ?: mutableListOf()

            if (itemPos !in list.indices) return@runTransaction null

            // 删除该次运动
            list.removeAt(itemPos)

            // 重算 totalBurn
            val newBurn = list.sumOf { (it["kcal"] as? Number)?.toInt() ?: 0 }

            tr.set(docRef, mapOf(
                "exercises" to list,
                "totalBurn"  to newBurn
            ), SetOptions.merge())
            null
        }.await()
    }


    data class DayData(
        val meals: List<Map<String, Any>> = emptyList(),
        val totalCalories: Int = 0,
        val exercises: List<Map<String, Any>> = emptyList(),
        val totalBurn: Int = 0
    )

    suspend fun loadDay(uid: String, date: String): DayData {
        val snap = user(uid).collection(COLL_RECORDS).document(date).get().await()
        val meals = (snap.get("meals") as? List<Map<String, Any>>)?.toList() ?: emptyList()
        val exercises = (snap.get("exercises") as? List<Map<String, Any>>)?.toList() ?: emptyList()
        val totalCalories = snap.getLong("totalCalories")?.toInt() ?: 0
        val totalBurn = snap.getLong("totalBurn")?.toInt() ?: 0
        return DayData(meals, totalCalories, exercises, totalBurn)
    }

    // Remote.kt  —— Records (Meals & Exercises) 区域内

    data class DailyEnergy(
        val date: String,          // "YYYY-MM-DD"
        val totalCalories: Int,    // 摄入
        val totalBurn: Int         // 消耗
    )

    /**
     * 订阅 [startDate, endDate]（含端点）的 records 文档，按日期升序返回每天的总摄入/总消耗。
     * 注意：文档ID就是 ISO 日期（YYYY-MM-DD），可用 FieldPath.documentId 进行范围查询。
     */
    fun observeRecordsRange(
        uid: String,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): kotlinx.coroutines.flow.Flow<List<DailyEnergy>> {
        val start = startDate.toString()
        val end   = endDate.toString()

        val coll = user(uid).collection(COLL_RECORDS)
        // 以 docId 作为排序/范围键（你的文档命名是 YYYY-MM-DD）
        val q = coll
            .orderBy(com.google.firebase.firestore.FieldPath.documentId(), com.google.firebase.firestore.Query.Direction.ASCENDING)
            .startAt(start)
            .endAt(end)

        // 你在其它地方已经有 listenDocs<> 的工具，这里复用它（见 BodyHistory 的实现）
        return listenDocs(q) { d ->
            DailyEnergy(
                date = d.id,
                totalCalories = (d.getLong("totalCalories") ?: 0L).toInt(),
                totalBurn     = (d.getLong("totalBurn")     ?: 0L).toInt()
            )
        }
    }

    // ------------------------------
    // Body metrics (type-centric layout)
    // ------------------------------

    suspend fun upsertBodyMetric(uid: String, type: String, date: String, value: Double, unit: String) {
        val col = user(uid).collection(COLL_BODY).document(type).collection(SUB_RECORDS)
        val doc = col.document(date)
        doc.set(
            mapOf(
                "value" to value,
                "unit" to unit,
                "date" to date,
                "ts" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun deleteBodyMetric(uid: String, type: String, date: String) {
        user(uid).collection(COLL_BODY).document(type).collection(SUB_RECORDS)
            .document(date).delete().await()
    }

    fun observeBodyHistory(uid: String, type: String): Flow<List<Map<String, Any?>>> {
        val q = user(uid).collection(COLL_BODY).document(type).collection(SUB_RECORDS)
            .orderBy("date", Query.Direction.ASCENDING)
        return listenDocs(q) { doc ->
            mapOf(
                "date" to (doc.getString("date") ?: doc.id),
                "value" to (doc.getDouble("value")),
                "unit" to (doc.getString("unit"))
            )
        }
    }

    fun observeBodyLatest(uid: String, type: String): Flow<Map<String, Any?>?> {
        val q = user(uid).collection(COLL_BODY).document(type).collection(SUB_RECORDS)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
        return listenDocs(q) { doc ->
            mapOf(
                "date" to (doc.getString("date") ?: doc.id),
                "value" to (doc.getDouble("value")),
                "unit" to (doc.getString("unit"))
            )
        }.map { it.firstOrNull() }
    }

    // ------------------------------
    // Growth / Tree
    // ------------------------------

    data class TreeState(
        val level: Int = 1,
        val fed: Int = 0,
        val pending: Int = 0
    )

    data class FeedResult(
        val consumed: Int,        // how much pending consumed this time
        val newLevel: Int,
        val newFed: Int,          // fed within current level
        val leveledUp: Int        // how many levels gained
    )

    enum class TaskId(val id: String, val defaultReward: Int) {
        LOGIN("login", 10),
        WATER("water", 20),
        MEAL("meal", 20),
        WORKOUT("workout", 20),
        BODY("body", 20),
        TEST("test", 1),
        FEED("feed", 1000),
        GIFT_ONCE("gift_once", 10),
        CLAIM_ONCE("claim_once", 10),
        GROUP_CHECKIN("group_checkin", 10);

        companion object {
            val ordered = listOf(LOGIN,WATER, MEAL, WORKOUT, BODY, TEST,FEED, GIFT_ONCE, CLAIM_ONCE, GROUP_CHECKIN)
            fun from(id: String): TaskId? = ordered.firstOrNull { it.id == id }
        }
    }

    data class TaskState(
        val id: String,
        val completed: Boolean,
        val claimed: Boolean,
        val reward: Int
    )

    suspend fun initTreeIfAbsent(uid: String) {
        val ref = user(uid).collection(COLL_GROWTH).document(DOC_TREE)
        db.runTransaction { tr ->
            val snap = tr.get(ref)
            if (!snap.exists()) {
                tr.set(
                    ref, mapOf(
                        "level" to 1,
                        "fed" to 0,
                        "pending" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            null
        }.await()
    }

    fun observeTree(uid: String): Flow<TreeState> = callbackFlow {
        val ref = user(uid).collection(COLL_GROWTH).document(DOC_TREE)
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) { trySend(TreeState()); return@addSnapshotListener }
            val level = snap?.getLong("level")?.toInt() ?: 1
            val fed = snap?.getLong("fed")?.toInt() ?: 0
            val pending = snap?.getLong("pending")?.toInt() ?: 0
            trySend(TreeState(level, fed, pending))
        }
        awaitClose { reg.remove() }
    }

    suspend fun feedAll(uid: String): FeedResult {
        val ref = user(uid).collection(COLL_GROWTH).document(DOC_TREE)
        val result = db.runTransaction { tr ->
            val snap = tr.get(ref)
            val level = snap.getLong("level")?.toInt() ?: 1
            val fed = snap.getLong("fed")?.toInt() ?: 0
            val pending = snap.getLong("pending")?.toInt() ?: 0
            if (pending <= 0) return@runTransaction FeedResult(0, level, fed, 0)

            val newFed = fed + pending
            tr.update(
                ref, mapOf(
                    "fed" to newFed,
                    "pending" to 0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            // 🚫 不在这里升级；升级改为动画结束后再扣 1000、level+1
            FeedResult(pending, level, newFed, 0)
        }.await()
        return result
    }


    // 动画结束后调用：扣除 1000 fed 并 level+1；fed 不够返回 false
    suspend fun upgrade(uid: String): Boolean {
        val ref = user(uid).collection(COLL_GROWTH).document(DOC_TREE)
        return db.runTransaction { tr ->
            val snap = tr.get(ref)
            val level = snap.getLong("level")?.toInt() ?: 1
            val fed = snap.getLong("fed")?.toInt() ?: 0
            if (fed < LEVEL_GOAL) return@runTransaction false

            tr.update(
                ref, mapOf(
                    "level" to (level + 1),
                    "fed" to (fed - LEVEL_GOAL),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            true
        }.await()
    }

    // 忽视 fed，强制 level+1（用于“成长”按钮）
    suspend fun forceLevelUp(uid: String): Boolean {
        val ref = user(uid).collection(COLL_GROWTH).document(DOC_TREE)
        return db.runTransaction { tr ->
            val snap = tr.get(ref)
            val level = snap.getLong("level")?.toInt() ?: 0
            tr.set(ref, mapOf(
                "level" to (level + 1),
                "updatedAt" to FieldValue.serverTimestamp()
            ), SetOptions.merge())
            true
        }.await()
    }

    // 忽视 fed，把 level 设为 0（用于“重置”按钮）
    // 如需重置时把 fed 一并清零，可在 map 中加入 "fed" to 0
    suspend fun resetLevel0(uid: String): Boolean {
        val ref = user(uid).collection(COLL_GROWTH).document(DOC_TREE)
        return db.runTransaction { tr ->
            tr.set(ref, mapOf(
                "level" to 0,
                "updatedAt" to FieldValue.serverTimestamp()
            ), SetOptions.merge())
            true
        }.await()
    }

    // ===== Water =====

    data class WaterEntry(val ml: Int, val ts: com.google.firebase.Timestamp)

    data class WaterDay(
        val date: String,
        val totalMl: Int = 0,
        val entries: List<WaterEntry> = emptyList()
    )

    suspend fun setWaterGoal(uid: String, goalMl: Int) {
        val ref = user(uid).collection("water").document("settings")
        ref.set(
            mapOf("goalMl" to goalMl, "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
    }

    suspend fun getWaterGoal(uid: String): Int {
        val ref = user(uid).collection("water").document("settings").get().await()
        return (ref.getLong("goalMl") ?: 0L).toInt().coerceAtLeast(0)
    }

    private fun todayStr(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    suspend fun addWater(uid: String, date: String = todayStr(), ml: Int): Boolean {
        if (ml <= 0) return false
        val dayRef = user(uid).collection("water").document("daily").collection("days").document(date)
        db.runTransaction { tr ->
            val snap = tr.get(dayRef)
            val old = (snap.getLong("totalMl") ?: 0L).toInt()
            val now = com.google.firebase.Timestamp.now()
            val newEntry = mapOf("ml" to ml, "ts" to now)
            tr.set(dayRef, mapOf(
                "totalMl" to (old + ml),
                "date" to date,
                "updatedAt" to FieldValue.serverTimestamp()
            ), SetOptions.merge())
            tr.update(dayRef, mapOf(
                "entries" to FieldValue.arrayUnion(newEntry)
            ))
            null
        }.await()
        // 写完后刷新“日常任务：喝水”
        runCatching { markTaskCompleted(uid, date, TaskId.WATER) }
        return true
    }

    fun observeWaterDay(uid: String, date: String = todayStr()): kotlinx.coroutines.flow.Flow<WaterDay?> =
        listenDoc(user(uid).collection("water").document("daily").collection("days").document(date)) { d ->
            val total = (d.getLong("totalMl") ?: 0L).toInt()
            val list = (d.get("entries") as? List<Map<String, Any?>>).orEmpty().mapNotNull { m ->
                val ml = (m["ml"] as? Number)?.toInt() ?: return@mapNotNull null
                val ts = (m["ts"] as? com.google.firebase.Timestamp) ?: com.google.firebase.Timestamp.now()
                WaterEntry(ml, ts)
            }.sortedByDescending { it.ts }
            WaterDay(date = d.getString("date") ?: d.id, totalMl = total, entries = list)
        }

    fun observeWaterHistory(uid: String, limit: Int = 30): kotlinx.coroutines.flow.Flow<List<WaterDay>> {
        val q = user(uid).collection("water").document("daily").collection("days")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        return listenDocs(q) { d ->
            val total = (d.getLong("totalMl") ?: 0L).toInt()
            WaterDay(date = d.getString("date") ?: d.id, totalMl = total, entries = emptyList())
        }
    }

    suspend fun deleteWater(uid: String, ts: Long, date: String = todayStr()) {
        val dayRef = user(uid).collection("water").document("daily").collection("days").document(date)

        db.runTransaction { tr ->
            val snap = tr.get(dayRef)
            val entries = (snap.get("entries") as? List<Map<String, Any?>>).orEmpty()
            val target = entries.firstOrNull {
                val its = it["ts"] as? com.google.firebase.Timestamp
                its?.toDate()?.time == ts
            } ?: return@runTransaction null

            val ml = (target["ml"] as? Number)?.toInt() ?: 0
            val oldTotal = (snap.getLong("totalMl") ?: 0L).toInt()

            // 更新总量
            tr.update(dayRef, "totalMl", (oldTotal - ml).coerceAtLeast(0))
            // 移除该条 entry
            tr.update(dayRef, "entries", FieldValue.arrayRemove(target))
            null
        }.await()
    }




// ------------------------------
// Daily Tasks
// ------------------------------

    private fun taskItemsCol(uid: String, date: String) =
        user(uid).collection(COLL_DAILY_TASKS).document(date).collection(SUB_ITEMS)

    fun observeDailyTasks(uid: String, date: String): Flow<List<TaskState>> = callbackFlow {
        val reg = taskItemsCol(uid, date).addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(TaskId.ordered.map { TaskState(it.id, false, false, it.defaultReward) })
                return@addSnapshotListener
            }
            val map = mutableMapOf<String, TaskState>()
            snap?.documents?.forEach { d ->
                val id = d.id
                val completed = d.getBoolean("completed") ?: false
                val claimed = d.getBoolean("claimed") ?: false
                val reward =
                    (d.getLong("reward") ?: TaskId.from(id)?.defaultReward?.toLong() ?: 0L).toInt()
                map[id] = TaskState(id, completed, claimed, reward)
            }
            val list = TaskId.ordered.map { t ->
                when (t) {
                    TaskId.TEST -> TaskState(t.id, completed = true, claimed = false, reward = 1)
                    TaskId.FEED -> TaskState(t.id, completed = true, claimed = false, reward = 1000)
                    else -> map[t.id] ?: TaskState(t.id, false, false, t.defaultReward)
                }
            }
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun refreshDailyCompletion(uid: String, date: String) {
        // meal/workout from records/{date}
        val rec = user(uid).collection(COLL_RECORDS).document(date).get().await()
        val hasMeal = (rec.get("meals") as? List<*>)?.isNotEmpty() == true
        val hasWorkout = (rec.get("exercises") as? List<*>)?.isNotEmpty() == true
        if (hasMeal) markTaskCompleted(uid, date, TaskId.MEAL)
        if (hasWorkout) markTaskCompleted(uid, date, TaskId.WORKOUT)

        // body metric: check common types
        val types = BODY_TYPES_CANDIDATES
        val hasBody = types.any { type ->
            val doc =
                user(uid).collection(COLL_BODY).document(type).collection(SUB_RECORDS).document(date)
            runCatching { doc.get().await().exists() }.getOrDefault(false)
        }
        if (hasBody) markTaskCompleted(uid, date, TaskId.BODY)
    }

    suspend fun markTaskCompleted(uid: String, date: String, id: TaskId) {
        val doc = taskItemsCol(uid, date).document(id.id)
        doc.set(
            mapOf(
                "completed" to true,
                "reward" to id.defaultReward,
                "updatedAt" to FieldValue.serverTimestamp(),
                "id" to id.id
            ), SetOptions.merge()
        ).await()
    }

    suspend fun claimTask(uid: String, date: String, id: TaskId): Int {
        // --- TEST: 可无限领取，每次 +1，不依赖任务文档 ---
        if (id == TaskId.TEST) {
            val treeRef = user(uid).collection(COLL_GROWTH).document(DOC_TREE)
            return db.runTransaction { tr ->
                // READS
                val treeSnap = tr.get(treeRef)
                val pendingNow = treeSnap.getLong("pending")?.toInt() ?: 0

                // WRITES
                if (!treeSnap.exists()) {
                    tr.set(treeRef, mapOf("level" to 1, "fed" to 0), SetOptions.merge())
                }
                tr.set(
                    treeRef,
                    mapOf(
                        "pending" to (pendingNow + 1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                1
            }.await()
        }
        if (id == TaskId.FEED) {
            val treeRef = user(uid).collection(COLL_GROWTH).document(DOC_TREE)
            return db.runTransaction { tr ->
                // READS
                val treeSnap = tr.get(treeRef)
                val pendingNow = treeSnap.getLong("pending")?.toInt() ?: 0

                // WRITES
                if (!treeSnap.exists()) {
                    tr.set(treeRef, mapOf("level" to 1, "fed" to 0), SetOptions.merge())
                }
                tr.set(
                    treeRef,
                    mapOf(
                        "pending" to (pendingNow + 1000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                1000
            }.await()
        }

        // --- 其他任务：正常“读完再写”的事务，避免 Firestore 的读写顺序限制 ---
        val taskRef = taskItemsCol(uid, date).document(id.id)
        val treeRef = user(uid).collection(COLL_GROWTH).document(DOC_TREE)

        return db.runTransaction { tr ->
            // READS（必须在任何写入前完成）
            val taskSnap = tr.get(taskRef)
            val treeSnap = tr.get(treeRef)

            if (!taskSnap.exists()) return@runTransaction 0
            val completed = taskSnap.getBoolean("completed") ?: false
            val claimed = taskSnap.getBoolean("claimed") ?: false
            val rewardLocal = (taskSnap.getLong("reward") ?: id.defaultReward.toLong()).toInt()
            if (!completed || claimed || rewardLocal <= 0) return@runTransaction 0

            val pendingNow = treeSnap.getLong("pending")?.toInt() ?: 0

            // WRITES
            tr.set(
                taskRef,
                mapOf("claimed" to true, "claimedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
            if (!treeSnap.exists()) {
                tr.set(treeRef, mapOf("level" to 1, "fed" to 0), SetOptions.merge())
            }
            tr.set(
                treeRef,
                mapOf(
                    "pending" to (pendingNow + rewardLocal),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            rewardLocal
        }.await()
    }

    // === 唯一ID ===

    data class UserProfile(
        val uid: String,
        val uniqueId: String? = null,     // 公开昵称（用户唯一ID）
        val uniqueIdLc: String? = null,   // lowercased 索引
        val avatarUrl: String? = null,
        val privacyFriendsReadable: Boolean = true,
        val privacyGroupsReadable: Boolean = true,
        val visibleFields: List<String> = emptyList()
    )

    private fun normalizeId(s: String) = s.trim().lowercase()
    private val UNIQUE_ID_REGEX = Regex("^[a-z0-9_]{3,20}$")

    /** 读取一次用户Profile（可能不存在） */
    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = user(uid).get().await()
        if (!doc.exists()) return null
        return UserProfile(
            uid = uid,
            uniqueId = doc.getString("uniqueId"),
            uniqueIdLc = doc.getString("uniqueIdLc"),
            avatarUrl = doc.getString("avatarUrl"),
            privacyFriendsReadable = doc.getBoolean("privacyFriendsReadable") ?: true,
            privacyGroupsReadable = doc.getBoolean("privacyGroupsReadable") ?: true,
            visibleFields = (doc.get("visibleFields") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }

    /** 监听用户Profile变化 */
    fun observeUserProfile(uid: String): kotlinx.coroutines.flow.Flow<UserProfile?> =
        listenDoc(user(uid)) { d ->
            UserProfile(
                uid = uid,
                uniqueId = d.getString("uniqueId"),
                uniqueIdLc = d.getString("uniqueIdLc"),
                avatarUrl = d.getString("avatarUrl"),
                privacyFriendsReadable = d.getBoolean("privacyFriendsReadable") ?: true,
                privacyGroupsReadable = d.getBoolean("privacyGroupsReadable") ?: true,
                visibleFields = (d.get("visibleFields") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }

    // 让 listenDocs 支持 DocumentReference
    private fun <T> listenDocs(
        ref: com.google.firebase.firestore.DocumentReference,
        mapper: (com.google.firebase.firestore.DocumentSnapshot) -> T
    ): Flow<List<T>> = callbackFlow {
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = if (snap != null && snap.exists()) listOf(mapper(snap)) else emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    /** 检查唯一ID是否可用（大小写不敏感；格式校验） */
    suspend fun isUniqueIdAvailable(id: String): Boolean {
        val norm = normalizeId(id)
        if (!UNIQUE_ID_REGEX.matches(norm)) return false
        val r = db.collection(COLL_USERNAMES).document(norm).get().await()
        val used = r.exists()
        android.util.Log.d("UniqueId", "check '$id' -> ${!used}")
        return !used
    }

    /** 设置/修改唯一ID（事务保证唯一；会释放旧ID占用） */
    // Remote.kt —— 完整替换
    suspend fun setUniqueId(uid: String, newIdRaw: String): Boolean {
        val newId = normalizeId(newIdRaw)
        if (!UNIQUE_ID_REGEX.matches(newId)) return false

        val userRef = user(uid)
        val nameRef = db.collection(COLL_USERNAMES).document(newId)

        return db.runTransaction { tr ->
            // 读
            val nameSnap = tr.get(nameRef)
            val userSnap = tr.get(userRef)
            val oldLc = userSnap.getString("uniqueIdLc")
            val oldRef = if (!oldLc.isNullOrBlank() && oldLc != newId)
                db.collection(COLL_USERNAMES).document(oldLc) else null
            val oldSnap = oldRef?.let { tr.get(it) }

            if (nameSnap.exists() && nameSnap.getString("uid") != uid) return@runTransaction false

            // 占位新ID
            tr.set(nameRef, mapOf("uid" to uid, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())

            // 释放旧ID（只在旧映射仍指向自己时）
            if (oldRef != null && oldSnap?.exists() == true && oldSnap.getString("uid") == uid) {
                tr.delete(oldRef)
            }

            // 写 users/{uid}
            val initPrivacyFriends = userSnap.get("privacyFriendsReadable") == null
            val initPrivacyGroups  = userSnap.get("privacyGroupsReadable") == null

            val data = mutableMapOf<String, Any>(
                "uniqueId" to newIdRaw.trim(),
                "uniqueIdLc" to newId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (initPrivacyFriends) data["privacyFriendsReadable"] = true
            if (initPrivacyGroups)  data["privacyGroupsReadable"]  = true

            tr.set(userRef, data, SetOptions.merge())
            true
        }.await().also {
            android.util.Log.d("UniqueId", "set '$newIdRaw' result=$it")
        }
    }


    // ---------- Friends (minimal) ----------
    // ---------- Friends (FriendInfo 扩充) ----------
    data class FriendInfo(
        val uid: String,
        val uniqueId: String? = null,
        val avatarUrl: String? = null,   // 你现在没用头像，保留不影响
        val treeLevel: Int = 0,
        // 新增：互动相关
        val pendingFromFriend: Int = 0,  // 这个好友送给“我”但我尚未领取的点数
        val lastGiftToFriend: com.google.firebase.Timestamp? = null // 我今天是否已赠送给“他”
    )


    data class FriendRequest(
        val fromUid: String,
        val fromUniqueId: String? = null,
        val ts: com.google.firebase.Timestamp? = null
    )

    // 监听我已添加的好友（去重 & 按时间）
    fun observeFriends(uid: String): Flow<List<FriendInfo>> {
        val col = user(uid).collection(COLL_FRIENDS).orderBy("ts", Query.Direction.DESCENDING)
        return listenDocs(col) { d ->
            FriendInfo(
                uid = d.id,
                uniqueId = d.getString("uniqueId"),
                avatarUrl = d.getString("avatarUrl"),
                treeLevel = (d.getLong("treeLevel") ?: 0L).toInt(),
                pendingFromFriend = (d.getLong("pendingFromFriend") ?: 0L).toInt(),
                lastGiftToFriend = d.getTimestamp("lastGiftToFriend")
            )
        }
    }

    // 监听“别人发给我”的待处理请求
    fun observeFriendRequests(uid: String): Flow<List<FriendRequest>> {
        val col = user(uid).collection(COLL_FRIEND_REQUESTS)
            .orderBy("ts", Query.Direction.DESCENDING)
        return listenDocs(col) { d ->
            FriendRequest(
                fromUid = d.id,
                fromUniqueId = d.getString("fromUniqueId"),
                ts = d.getTimestamp("ts")
            )
        }
    }

    // 通过对方的唯一ID发起好友申请
// Remote.kt ——（若你已按我上次版本改过，可只加日志）
    suspend fun sendFriendRequest(fromUid: String, toUniqueId: String): Boolean {
        val toLc = toUniqueId.trim().lowercase()
        val mapping = db.collection(COLL_USERNAMES).document(toLc).get().await()
        val toUid = mapping.getString("uid") ?: run {
            android.util.Log.w("Friends", "send: username '$toLc' not found")
            return false
        }
        if (toUid == fromUid) return false

        val meRef = user(fromUid)
        val toRef = user(toUid)
        val reqRef = toRef.collection(COLL_FRIEND_REQUESTS).document(fromUid)

        val myProfile = meRef.get().await()
        val myUniqueId = myProfile.getString("uniqueId")

        reqRef.set(
            mapOf("fromUniqueId" to myUniqueId, "ts" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()

        android.util.Log.d("Friends", "send: from=$fromUid(${myUniqueId}) -> to=$toUid(username=$toLc)")
        return true
    }

    // Remote.kt —— 替换
    suspend fun respondFriendRequest(meUid: String, fromUid: String, accept: Boolean): Boolean {
        val meRef   = user(meUid)              // B
        val fromRef = user(fromUid)            // A
        val reqRef  = meRef.collection(COLL_FRIEND_REQUESTS).document(fromUid) // /users/B/friend_requests/A

        return db.runTransaction { tr ->
            // 1) 读取：只读“请求本身”和“我自己的 profile”
            val reqSnap = tr.get(reqRef)
            if (!reqSnap.exists()) return@runTransaction false

            val meProfile  = tr.get(meRef)                      // 允许读
            val meUniqueId = meProfile.getString("uniqueId")
            val meAvatar   = meProfile.getString("avatarUrl")

            if (accept) {
                // 来自请求里的对方唯一ID（A 在发起时写进来的），避免去读对方 users/{A}
                val fromUniqueId = reqSnap.getString("fromUniqueId") ?: fromUid

                // 2) 先写双方 friends（merge 幂等）
                // 2.1 我(B)这边的 friends/A —— 本人写自己，规则直接允许
                tr.set(
                    meRef.collection(COLL_FRIENDS).document(fromUid),
                    mapOf(
                        "uniqueId"  to fromUniqueId,
                        "avatarUrl" to null,                     // 暂不填；以后可由对方主动更新
                        "treeLevel" to 0,                        // 或省略该字段
                        "ts"       to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                // 2.2 对方(A)的 friends/B —— 由我写；规则要求 /users/A 这边仍存在 /friend_requests/B
                tr.set(
                    fromRef.collection(COLL_FRIENDS).document(meUid),
                    mapOf(
                        "uniqueId"  to meUniqueId,
                        "avatarUrl" to meAvatar,
                        "ts"        to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }

            // 3) 最后删除我这边的请求（确保前面的 2.2 能通过规则里的 exists() 判断）
            tr.delete(reqRef)

            true
        }.await().also {
            android.util.Log.d("Friends", "respondFriendRequest(me=$meUid, from=$fromUid, accept=$accept) -> $it")
        }
    }


    // ====== Friends: 双向删除 ======
    suspend fun removeFriend(meUid: String, otherUid: String): Boolean {
        val myDoc     = user(meUid).collection(COLL_FRIENDS).document(otherUid)
        val otherDoc  = user(otherUid).collection(COLL_FRIENDS).document(meUid)

        return try {
            val batch = db.batch()
            batch.delete(myDoc)    // /users/{me}/friends/{other}   （本人删自己 -> 允许）
            batch.delete(otherDoc) // /users/{other}/friends/{me}   （我删对方侧 -> 规则已放开）
            batch.commit().await()
            android.util.Log.d("Friends", "removeFriendBothWays: $meUid <-> $otherUid deleted")
            true
        } catch (e: Exception) {
            android.util.Log.e("Friends", "removeFriendBothWays failed", e)
            false
        }
    }


    // === 好友公开树状态（只读 level & fed） ===
    data class TreePublic(
        val level: Int = 0,
        val fed: Int = 0
    )

    fun observeTreePublic(uid: String): kotlinx.coroutines.flow.Flow<TreePublic?> =
        listenDoc(user(uid).collection(COLL_GROWTH).document(DOC_TREE)) { d ->
            TreePublic(
                level = (d.getLong("level") ?: 0L).toInt(),
                fed   = (d.getLong("fed")   ?: 0L).toInt()
            )
        }

    // ========== Gifting APIs ==========


    // 工具：把当前日期折算成 yyyymmdd，便于“每日归零”
    private fun todayKey(): Int {
        val cal = java.util.Calendar.getInstance()
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return y * 10000 + m * 100 + d
    }

    /**
     * 我(meUid) 赠送给 好友(friendUid)
     * - 限制：每天每个好友最多赠送 1 次（5点）
     * - 实现：事务
     *   1) 读我这边 friends/{friend} 的 lastGiftToFriend 是否今日
     *   2) 给对方那边 friends/{me} 的 pendingFromFriend += 5，lastGiftFromFriend=now（跨写，已在规则中放开）
     *   3) 我这边 friends/{friend}.lastGiftToFriend = now
     */
    // 我(meUid) 赠送给 好友(friendUid)
// - 每个好友每天最多赠送一次（+5）
// - 不读取对方文档，直接原子自增对方 pendingFromFriend
    suspend fun giftToFriend(meUid: String, friendUid: String): Boolean {
        val meDoc = user(meUid).collection(COLL_FRIENDS).document(friendUid)        // /users/me/friends/friend
        val frDocOnFriend = user(friendUid).collection(COLL_FRIENDS).document(meUid) // /users/friend/friends/me
        val now = com.google.firebase.Timestamp.now()

        return db.runTransaction { tr ->
            // 只读取“我自己的”friends/{friend}，检查今天是否已送
            val mySnap = tr.get(meDoc)
            val lastGiftTs = mySnap.getTimestamp("lastGiftToFriend")
            if (lastGiftTs != null) {
                val c = java.util.Calendar.getInstance().apply { time = lastGiftTs.toDate() }
                val giftedKey = c.get(java.util.Calendar.YEAR) * 10000 +
                        (c.get(java.util.Calendar.MONTH) + 1) * 100 +
                        c.get(java.util.Calendar.DAY_OF_MONTH)
                val nowC = java.util.Calendar.getInstance()
                val todayKey = nowC.get(java.util.Calendar.YEAR) * 10000 +
                        (nowC.get(java.util.Calendar.MONTH) + 1) * 100 +
                        nowC.get(java.util.Calendar.DAY_OF_MONTH)
                if (giftedKey == todayKey) {
                    return@runTransaction false // 今天已赠送过该好友
                }
            }

            // ✅ 不读取对方文档，直接原子自增
            tr.set(
                frDocOnFriend,
                mapOf(
                    "pendingFromFriend" to FieldValue.increment(GIFT_UNIT.toLong()),
                    "lastGiftFromFriend" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            // 在我这边标记今日已送
            tr.set(
                meDoc,
                mapOf(
                    "lastGiftToFriend" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            true
        }.await().also {
            android.util.Log.d("FriendsGift", "giftToFriend me=$meUid -> friend=$friendUid result=$it")
        }
    }


    /**
     * 我(meUid) 向某个好友领取（从我这边 friends/{friend}.pendingFromFriend 扣除）
     * - 限制：每天领取总量最多 50
     * - 实现：事务
     *   1) 读 /users/me/growth/tree 的 giftReceived & giftDate（若不是今天则清零）
     *   2) 读 /users/me/friends/{friend}.pendingFromFriend
     *   3) 计算可领数 = min(pending, 50 - receivedToday)
     *   4) 增加树 fed，更新 giftReceived，扣减 pending
     */
    suspend fun claimFromFriend(meUid: String, friendUid: String): Int {
        val myFriendDoc = user(meUid).collection(COLL_FRIENDS).document(friendUid)
        val treeDoc = user(meUid).collection(COLL_GROWTH).document(DOC_TREE)
        val today = todayKey()

        return db.runTransaction { tr ->
            val treeSnap = tr.get(treeDoc)
            var received = (treeSnap.getLong("giftReceived") ?: 0L).toInt()
            var dateKey  = (treeSnap.getLong("giftDate") ?: 0L).toInt()
            if (dateKey != today) {
                received = 0
                dateKey = today
            }
            val remain = (DAILY_CLAIM_LIMIT - received).coerceAtLeast(0)

            val frSnap = tr.get(myFriendDoc)
            val pending = (frSnap.getLong("pendingFromFriend") ?: 0L).toInt()
            val take = pending.coerceAtMost(remain)
            if (take <= 0) return@runTransaction 0

            val fedOld = (treeSnap.getLong("fed") ?: 0L).toInt()
            tr.set(
                treeDoc,
                mapOf(
                    "fed" to (fedOld + take),
                    "giftReceived" to (received + take),
                    "giftDate" to dateKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            tr.set(
                myFriendDoc,
                mapOf(
                    "pendingFromFriend" to (pending - take),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            take
        }.await().also {
            android.util.Log.d("FriendsGift", "claimFromFriend me=$meUid <- friend=$friendUid take=$it")
        }
    }

    /** 一键赠送：对所有好友尝试送一次（逐个调用事务） */
    suspend fun giftAll(meUid: String, friends: List<String>): Pair<Int, Int> {
        var ok = 0; var fail = 0
        for (f in friends) {
            runCatching { giftToFriend(meUid, f) }.onSuccess { if (it) ok++ else fail++ }.onFailure { fail++ }
        }
        return ok to fail
    }

    /** 一键领取：从所有好友处领取，直到达到每日上限 */
    suspend fun claimAll(meUid: String, friends: List<String>): Int {
        var total = 0
        for (f in friends) {
            val got = runCatching { claimFromFriend(meUid, f) }.getOrDefault(0)
            total += got
            if (total >= DAILY_CLAIM_LIMIT) break
        }
        return total
    }

    // ===== Rooms =====

    data class RoomInfo(
        val id: String,
        val name: String,
        val ownerUid: String,
        val createdAt: com.google.firebase.Timestamp? = null,
        val role: String? = null // 从 users/{uid}/rooms 清单里带过来的角色：owner/member
    )

    // 生成 6 位 ID（避免碰撞）
    private suspend fun genRoomId(): String {
        val rnd = java.security.SecureRandom()
        repeat(10) {
            val id = java.math.BigInteger(30, rnd).toString(36).padStart(6, '0').take(6)
            val snap = db.collection("rooms").document(id).get().await()
            if (!snap.exists()) return id
        }
        return db.collection("rooms").document().id.take(8)
    }

    suspend fun createRoom(ownerUid: String, name: String): String {
        val roomId = genRoomId()
        val roomRef = db.collection("rooms").document(roomId)
        val memberRef = roomRef.collection("members").document(ownerUid)
        val userRoomRef = user(ownerUid).collection("rooms").document(roomId)

        try {
            db.runBatch { b ->
                b.set(roomRef, mapOf(
                    "name" to name,
                    "ownerUid" to ownerUid,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
                b.set(memberRef, mapOf(
                    "role" to "owner",
                    "joinedAt" to FieldValue.serverTimestamp()
                ))
                b.set(userRoomRef, mapOf(
                    "name" to name,
                    "role" to "owner",
                    "joinedAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            android.util.Log.d("Rooms", "createRoom OK id=$roomId")
            return roomId
        } catch (e: Exception) {
            android.util.Log.e("Rooms", "createRoom FAIL id=$roomId", e)
            throw e
        }
    }


    // 观察“我加入的房间”清单（users/{uid}/rooms）
    fun observeMyRooms(uid: String): Flow<List<RoomInfo>> {
        val col = user(uid).collection(COLL_USER_ROOMS).orderBy("joinedAt", Query.Direction.DESCENDING)
        return listenDocs(col) { d ->
            RoomInfo(
                id = d.id,
                name = d.getString("name") ?: d.id,
                ownerUid = "", // 清单里没 ownerUid，UI 仅展示用
                createdAt = null,
                role = d.getString("role")
            )
        }
    }

    // 按 roomId 精确查找（加入用）；也支持按 name 等值匹配
    suspend fun findRoomByIdOrName(q: String): List<RoomInfo> {
        val id = q.trim()
        val ref = db.collection(COLL_ROOMS)

        val byId = ref.document(id).get().await()
        val list = mutableListOf<RoomInfo>()
        if (byId.exists()) {
            list += RoomInfo(
                id = byId.id,
                name = byId.getString("name") ?: byId.id,
                ownerUid = byId.getString("ownerUid") ?: ""
            )
            return list
        }
        // name 等值匹配（注意需要索引时在控制台创建）
        val byName = ref.whereEqualTo("name", id).get().await()
        for (d in byName.documents) {
            list += RoomInfo(
                id = d.id,
                name = d.getString("name") ?: d.id,
                ownerUid = d.getString("ownerUid") ?: ""
            )
        }
        return list
    }

    // 加入房间（本人写自己的 member 文档 + 用户清单）
    suspend fun joinRoom(uid: String, roomId: String): Boolean {
        val roomRef = db.collection(COLL_ROOMS).document(roomId)
        val exist = roomRef.get().await()
        if (!exist.exists()) return false

        val myMember = roomRef.collection(COLL_MEMBERS).document(uid)
        val userRoom = user(uid).collection(COLL_USER_ROOMS).document(roomId)
        val name = exist.getString("name") ?: roomId

        db.runBatch { b ->
            b.set(myMember, mapOf("role" to "member", "joinedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            b.set(userRoom, mapOf("name" to name, "role" to "member", "joinedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
        }.await()
        return true
    }

    // 离开房间：普通成员直接删自己；房主不允许离开（必须解散）
    suspend fun leaveRoom(uid: String, roomId: String): Boolean {
        val roomRef = db.collection(COLL_ROOMS).document(roomId)
        val snap = roomRef.get().await()
        if (!snap.exists()) return false
        val owner = snap.getString("ownerUid")
        if (owner == uid) throw IllegalStateException("房主不能直接退出，请先解散房间")

        val myMember = roomRef.collection(COLL_MEMBERS).document(uid)
        val userRoom = user(uid).collection(COLL_USER_ROOMS).document(roomId)

        db.runBatch { b ->
            b.delete(myMember)
            b.delete(userRoom)
        }.await()
        return true
    }

    // 解散房间：房主删除所有成员的清单 + members 子集合 + 房间文档
    suspend fun dissolveRoom(ownerUid: String, roomId: String): Boolean {
        val roomRef = db.collection(COLL_ROOMS).document(roomId)
        val snap = roomRef.get().await()
        if (!snap.exists()) return false
        val owner = snap.getString("ownerUid")
        if (owner != ownerUid) throw SecurityException("只有房主可以解散房间")

        // 取出所有成员
        val members = roomRef.collection(COLL_MEMBERS).get().await().documents

        // 批量删除：每批最多 500
        val chunks = members.chunked(400)
        for (chunk in chunks) {
            val batch = db.batch()
            for (m in chunk) {
                val uid = m.id
                batch.delete(roomRef.collection(COLL_MEMBERS).document(uid))
                batch.delete(user(uid).collection(COLL_USER_ROOMS).document(roomId))
            }
            batch.commit().await()
        }
        // 最后删房间文档
        roomRef.delete().await()
        return true
    }

    // Remote.kt 追加

    data class RoomMember(
        val uid: String,
        val role: String? = null,
        val checkDate: Int? = null
    )

    /** 监听房间成员（含角色、今日打卡字段） */
    fun observeRoomMembers(roomId: String): Flow<List<RoomMember>> {
        val col = db.collection(COLL_ROOMS).document(roomId).collection(COLL_MEMBERS)
        return listenDocs(col.orderBy("joinedAt", Query.Direction.DESCENDING)) { d ->
            RoomMember(
                uid = d.id,
                role = d.getString("role"),
                checkDate = (d.getLong("checkDate") ?: 0L).toInt()
            )
        }
    }

    /** 将今天打卡写到 members/{uid}.checkDate；返回是否“新打卡” */
    suspend fun checkinRoom(uid: String, roomId: String): Boolean {
        val roomRef = db.collection("rooms").document(roomId)
        val meMember = roomRef.collection("members").document(uid)

        val today = java.util.Calendar.getInstance().let { c ->
            c.get(java.util.Calendar.YEAR) * 10000 +
                    (c.get(java.util.Calendar.MONTH) + 1) * 100 +
                    c.get(java.util.Calendar.DAY_OF_MONTH)
        }

        return db.runTransaction { tr ->
            val snap = tr.get(meMember)
            if (!snap.exists()) throw IllegalStateException("not a member")
            val last = (snap.getLong("checkDate") ?: 0L).toInt()
            val changed = (last != today)
            if (changed) {
                tr.set(
                    meMember,
                    mapOf("checkDate" to today, "updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge() // ✅ 只改这两个字段，规则放行
                )
            }
            changed
        }.await()
    }


    // 监听单个房间，拿 name/ownerUid
    fun observeRoom(roomId: String): Flow<RoomInfo?> =
        listenDoc(db.collection("rooms").document(roomId)) { d ->
            RoomInfo(
                id = d.id,
                name = d.getString("name") ?: d.id,
                ownerUid = d.getString("ownerUid") ?: "",
                createdAt = d.getTimestamp("createdAt")
            )
        }

    // ============ Sleep ============

    data class SleepSession(
        val id: String = "",
        val startTs: com.google.firebase.Timestamp,
        val endTs: com.google.firebase.Timestamp,
        val durationMin: Int,
        val isNap: Boolean = false,
        val note: String? = null,
        val dayBucket: String, // 以“入睡日”为桶（用户本地时区）
    )

    data class SleepDaily(
        val date: String,            // YYYY-MM-DD
        val totalMin: Int = 0,
        val sessionsCount: Int = 0,
        val firstBedTimeMin: Int? = null // 当天第一段入睡时间（分钟，00:00起算）
    )

    suspend fun setSleepGoal(uid: String, goalMinutes: Int) {
        val ref = user(uid).collection("sleep").document("settings")
        ref.set(
            mapOf("goalMinutes" to goalMinutes.coerceAtLeast(0), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
    }

    suspend fun getSleepGoal(uid: String): Int {
        val ref = user(uid).collection("sleep").document("settings").get().await()
        return (ref.getLong("goalMinutes") ?: 480L).toInt().coerceAtLeast(0) // 默认 8h
    }

    // 将 java.time.Instant 转换为 Firebase Timestamp
    private fun Instant.toFs(): com.google.firebase.Timestamp =
        com.google.firebase.Timestamp(Date.from(this))

    private fun LocalDate.formatYMD(): String =
        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.format(this)

    // 新增一条睡眠会话（自动更新 dayBucket 对应的 daily 聚合）
    suspend fun addSleepSession(
        uid: String,
        start: Instant,
        end: Instant,
        isNap: Boolean = false,
        note: String? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        require(end.isAfter(start)) { "结束时间必须晚于开始时间" }

        val startLocal = start.atZone(zoneId)
        val endLocal = end.atZone(zoneId)

        // 入睡日作为 dayBucket（跨天睡眠也归入入睡当天）
        val bucketDate = startLocal.toLocalDate().formatYMD()
        val durationMin = Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)

        val col = user(uid).collection("sleep").document("sessions").collection("items")
        val newId = col.document().id
        val dailyRef = user(uid).collection("sleep").document("daily").collection("days").document(bucketDate)

        db.runTransaction { tr ->
            // 先读 daily
            val snap = tr.get(dailyRef)

            val oldTotal = (snap.getLong("totalMin") ?: 0L).toInt()
            val oldCount = (snap.getLong("sessionsCount") ?: 0L).toInt()
            val bedMinutes = startLocal.toLocalTime().let { it.hour * 60 + it.minute }
            val oldFirst = (snap.getLong("firstBedTimeMin") ?: Int.MAX_VALUE.toLong()).toInt()
            val newFirst = minOf(oldFirst, bedMinutes)

            // 再写 session
            val docRef = col.document(newId)
            tr.set(
                docRef,
                mapOf(
                    "startTs" to start.toFs(),
                    "endTs" to end.toFs(),
                    "durationMin" to durationMin,
                    "isNap" to isNap,
                    "note" to note,
                    "dayBucket" to bucketDate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 再写 daily
            val update = mutableMapOf<String, Any>(
                "date" to bucketDate,
                "totalMin" to (oldTotal + durationMin),
                "sessionsCount" to (oldCount + 1),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (oldFirst == Int.MAX_VALUE) update["firstBedTimeMin"] = bedMinutes
            else update["firstBedTimeMin"] = newFirst

            tr.set(dailyRef, update, SetOptions.merge())
            null
        }.await()


        return newId
    }

    // 观察一段时间内的 sessions（按 startTs 查询）
    fun observeSleepSessions(
        uid: String,
        from: Instant,
        to: Instant
    ): kotlinx.coroutines.flow.Flow<List<SleepSession>> {
        val q = user(uid).collection("sleep").document("sessions").collection("items")
            .whereGreaterThanOrEqualTo("startTs", from.toFs())
            .whereLessThan("startTs", to.toFs())
            .orderBy("startTs", Query.Direction.ASCENDING)

        return listenDocs(q) { d ->
            val startTs = d.getTimestamp("startTs")!!
            val endTs = d.getTimestamp("endTs")!!
            SleepSession(
                id = d.id,
                startTs = startTs,
                endTs = endTs,
                durationMin = (d.getLong("durationMin") ?: 0L).toInt(),
                isNap = d.getBoolean("isNap") ?: false,
                note = d.getString("note"),
                dayBucket = d.getString("dayBucket") ?: startTs.toDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate().formatYMD()
            )
        }
    }

    // 观察某个日期段的 daily（用于周/月统计）
    fun observeSleepDaily(
        uid: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): kotlinx.coroutines.flow.Flow<List<SleepDaily>> {
        val q = user(uid).collection("sleep").document("daily").collection("days")
            .whereGreaterThanOrEqualTo("date", startDate.formatYMD())
            .whereLessThanOrEqualTo("date", endDate.formatYMD())
            .orderBy("date", Query.Direction.ASCENDING)

        return listenDocs(q) { d ->
            SleepDaily(
                date = d.getString("date") ?: d.id,
                totalMin = (d.getLong("totalMin") ?: 0L).toInt(),
                sessionsCount = (d.getLong("sessionsCount") ?: 0L).toInt(),
                firstBedTimeMin = (d.getLong("firstBedTimeMin") ?: null)?.toInt()
            )
        }
    }

    suspend fun deleteSleepSession(
        uid: String,
        sessionId: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val col = user(uid).collection("sleep").document("sessions").collection("items")
        val docRef = col.document(sessionId)
        val snap = docRef.get().await()
        if (!snap.exists()) return false

        val startTs = snap.getTimestamp("startTs")!!
        val endTs   = snap.getTimestamp("endTs")!!
        val durationMin: Int =
            (snap.getLong("durationMin")?.toInt()
                ?: Duration.between(
                    startTs.toDate().toInstant(),
                    endTs.toDate().toInstant()
                ).toMinutes().toInt()
                    ).coerceAtLeast(0)

        val bucketDate = snap.getString("dayBucket") ?: startTs.toDate().toInstant()
            .atZone(zoneId).toLocalDate().formatYMD()

        val dailyRef = user(uid).collection("sleep").document("daily")
            .collection("days").document(bucketDate)

        var newCount = 0
        db.runTransaction { tr ->
            val daily = tr.get(dailyRef)
            val oldTotal: Int = (daily.getLong("totalMin") ?: 0L).toInt()
            val oldCount: Int = (daily.getLong("sessionsCount") ?: 0L).toInt()

            tr.delete(docRef)

            newCount = (oldCount - 1).coerceAtLeast(0)
            val newTotal = (oldTotal - durationMin).coerceAtLeast(0)
            val update = mutableMapOf<String, Any>(
                "date" to bucketDate,
                "totalMin" to newTotal,
                "sessionsCount" to newCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newCount == 0) update["firstBedTimeMin"] = FieldValue.delete()
            tr.set(dailyRef, update, SetOptions.merge())
            null
        }.await()

        if (newCount > 0) {
            // —— 无索引兜底：不 orderBy，取回后本地选最早 ——
            val remaining = col.whereEqualTo("dayBucket", bucketDate).get().await().documents
            val earliest = remaining.minByOrNull { it.getTimestamp("startTs")?.toDate()?.time ?: Long.MAX_VALUE }
            val firstBedMin = earliest?.getTimestamp("startTs")
                ?.toDate()?.toInstant()?.atZone(zoneId)?.toLocalTime()
                ?.let { it.hour * 60 + it.minute }
            if (firstBedMin != null) {
                dailyRef.set(
                    mapOf(
                        "firstBedTimeMin" to firstBedMin,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }
        }
        return true
    }

    // ------------------------------
    // Internals
    // ------------------------------

    private fun listenIdSet(colRef: com.google.firebase.firestore.CollectionReference): Flow<Set<String>> =
        callbackFlow {
            val reg = colRef.addSnapshotListener { snap, e ->
                if (e != null) {
                    trySend(emptySet()); return@addSnapshotListener
                }
                val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
            awaitClose { reg.remove() }
        }

    // Remote.kt —— 替换 DocumentReference 版本的 listenDocs
    // ====== 放在 class Remote {} 里 ======
    private fun <T> listenDoc(
        ref: com.google.firebase.firestore.DocumentReference,
        mapper: (com.google.firebase.firestore.DocumentSnapshot) -> T
    ): kotlinx.coroutines.flow.Flow<T?> = kotlinx.coroutines.flow.callbackFlow {
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) {
                android.util.Log.e("FSListen", "listenDoc error at ${ref.path}", e)
                // 关键：出错不发送值，保留现有 UI（避免闪成 0）
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                trySend(mapper(snap))
            } else {
                trySend(null) // 文档不存在时才发 null
            }
        }
        awaitClose { reg.remove() }
    }

    private fun <T> listenDocs(
        query: Query,
        mapper: (DocumentSnapshot) -> T
    ): Flow<List<T>> = callbackFlow {
        val reg = query.addSnapshotListener { snaps, e ->
            if (e != null) {
                android.util.Log.e("FSListen", "listenDocs error at ${query}", e)
                // 不发送空列表，保留现有 UI
                return@addSnapshotListener
            }
            val list = snaps?.documents?.map(mapper).orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

}

private const val COLL_USERS = "users"
private const val COLL_FAVORITES = "favorites"
private const val COLL_RECORDS = "records"
private const val COLL_BODY = "body_metrics"

// sub-documents/collections naming
private const val SUB_FOOD = "food"
private const val SUB_EXERCISE = "exercise"
private const val SUB_IDS = "items"      // favorites/{type}/items/{id}
private const val SUB_RECORDS = "records"

// legacy paths for backward-compat reads
private const val OLD_FOOD_FAV = "favorites"              // users/{uid}/favorites/{id}
private const val OLD_EXERCISE_FAV = "exercise_favorites" // users/{uid}/exercise_favorites/{id}

private const val COLL_GROWTH = "growth"
private const val DOC_TREE = "tree"
private const val COLL_DAILY_TASKS = "tasks"
private const val SUB_ITEMS = "items"
private const val LEVEL_GOAL = 1000

private val BODY_TYPES_CANDIDATES = listOf(
    "weight","waist","chest","hip","thigh","upperarm","height","bodyFat"
)

private const val COLL_USERNAMES = "usernames"  // usernames/{uniqueIdLc} -> { uid }

private const val COLL_FRIENDS = "friends"                 // users/{uid}/friends/{otherUid}
private const val COLL_FRIEND_REQUESTS = "friend_requests" // users/{uid}/friend_requests/{fromUid}

private const val GIFT_UNIT = 5          // 每次赠送 5 点
private const val DAILY_CLAIM_LIMIT = 50 // 每日最多领取 50 点

private const val COLL_ROOMS = "rooms"
private const val COLL_MEMBERS = "members"
private const val COLL_USER_ROOMS = "rooms"  // users/{uid}/rooms