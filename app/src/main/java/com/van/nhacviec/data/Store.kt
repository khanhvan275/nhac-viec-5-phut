package com.van.nhacviec.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.van.nhacviec.widget.NhacViecWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Toàn bộ dữ liệu lưu trong một file JSON trên điện thoại.
 * Cập nhật app (cài đè) vẫn giữ nguyên file này.
 */
object Store {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    private var file: File? = null
    private var appContext: Context? = null

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        appContext = context.applicationContext
        val f = File(context.applicationContext.filesDir, "data.json")
        file = f
        if (f.exists()) {
            runCatching { json.decodeFromString(AppData.serializer(), f.readText()) }
                .onSuccess { _data.value = it }
                .onFailure {
                    // Giữ lại bản lỗi để không mất dữ liệu.
                    f.copyTo(File(f.parentFile, "data-loi-${System.currentTimeMillis()}.json"), true)
                }
        }
    }

    val value: AppData get() = _data.value

    @Synchronized
    fun update(block: (AppData) -> AppData) {
        val next = block(_data.value)
        _data.value = next
        save(next)
        refreshWidget()
    }

    private fun save(d: AppData) {
        val f = file ?: return
        val tmp = File(f.parentFile, "data.json.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(f)) {
            f.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun refreshWidget() {
        val ctx = appContext ?: return
        scope.launch { runCatching { NhacViecWidget().updateAll(ctx) } }
    }

    // ---- Các thao tác tiện dụng ----

    fun upsertTask(t: Task) = update { d ->
        val exists = d.tasks.any { it.id == t.id }
        d.copy(tasks = if (exists) d.tasks.map { if (it.id == t.id) t else it } else d.tasks + t)
    }

    fun deleteTask(id: String) = update { d ->
        d.copy(
            tasks = d.tasks.filterNot { it.id == id },
            timer = if (d.timer?.taskId == id) null else d.timer,
        )
    }

    fun setDone(id: String, done: Boolean) = update { d ->
        val td = today()
        d.copy(tasks = d.tasks.map { t ->
            if (t.id != id) t
            else if (t.daily) t.copy(doneDays = if (done) (t.doneDays + td).distinct().takeLast(400) else t.doneDays - td)
            else t.copy(doneAt = if (done) System.currentTimeMillis() else null)
        })
    }

    fun snooze(id: String) = update { d ->
        d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(lastRemindAt = System.currentTimeMillis()) else it })
    }

    fun upsertCountdown(c: Countdown) = update { d ->
        val exists = d.countdowns.any { it.id == c.id }
        d.copy(countdowns = if (exists) d.countdowns.map { if (it.id == c.id) c else it } else d.countdowns + c)
    }

    fun deleteCountdown(id: String) = update { d -> d.copy(countdowns = d.countdowns.filterNot { it.id == id }) }

    fun setSettings(s: Settings) = update { it.copy(settings = s) }

    // ---- Lộ trình ----

    /** Lưu lộ trình và giữ việc hằng ngày đi kèm cho khớp. */
    fun savePlan(p: Plan) = update { d ->
        val plans = if (d.plans.any { it.id == p.id }) d.plans.map { if (it.id == p.id) p else it } else d.plans + p
        val existing = d.tasks.firstOrNull { it.id == p.taskId }
        val task = (existing ?: Task(id = p.taskId, title = p.name, quadrant = p.quadrant, daily = true, planId = p.id))
            .copy(title = p.name, quadrant = p.quadrant, daily = true, planId = p.id)
        val tasks = if (existing != null) d.tasks.map { if (it.id == task.id) task else it } else d.tasks + task
        d.copy(plans = plans, tasks = tasks)
    }

    fun deletePlan(id: String) = update { d ->
        val p = d.plan(id) ?: return@update d
        d.copy(
            plans = d.plans.filterNot { it.id == id },
            tasks = d.tasks.filterNot { it.id == p.taskId },
            timer = if (d.timer?.taskId == p.taskId) null else d.timer,
            pendingLog = if (d.pendingLog?.planId == id) null else d.pendingLog,
        )
    }

    private fun AppData.withPlan(id: String, f: (Plan) -> Plan): AppData =
        copy(plans = plans.map { if (it.id == id) f(it) else it })

    private fun AppData.markTaskDone(taskId: String): AppData {
        val td = today()
        return copy(tasks = tasks.map { t ->
            if (t.id == taskId) t.copy(doneDays = (t.doneDays + td).distinct().takeLast(400)) else t
        })
    }

    /** Ghi % đã học của một chương/bài. */
    fun logProgress(planId: String, stepId: String, percent: Int) = update { d ->
        val td = today()
        val p = d.plan(planId) ?: return@update d.copy(pendingLog = null)
        val next = d.withPlan(planId) { plan ->
            plan.copy(steps = plan.steps.map { s ->
                if (s.id != stepId) s
                else {
                    val before = if (s.progressDay == td) s.progressBefore else s.progress
                    val pct = percent.coerceIn(0, 100)
                    val finishedNow = pct >= 100 && s.doneDay == null
                    s.copy(
                        progress = pct,
                        progressBefore = before,
                        progressDay = td,
                        doneDay = if (finishedNow) td else s.doneDay,
                        reviewStage = if (finishedNow) 0 else s.reviewStage,
                        nextReview = if (finishedNow) td + REVIEW_DAYS.first() else s.nextReview,
                    )
                }
            })
        }
        next.markTaskDone(p.taskId).copy(pendingLog = null)
    }

    /** Ghi phút tập luyện hôm nay. Đủ 5 phút thì đạt và mai chuyển sang buổi kế tiếp. */
    fun logExercise(planId: String, minutes: Int): PendingLog? {
        var result: PendingLog? = null
        update { d ->
            val td = today()
            val p = d.plan(planId) ?: return@update d
            val step = p.currentStep(td) ?: return@update d
            val logs = (p.exerciseLogs + ExerciseLog(td, step.id, minutes)).takeLast(1000)
            val total = logs.filter { it.day == td }.sumOf { it.minutes }
            val passNow = total >= EXERCISE_PASS_MIN && p.lastPassDay != td
            val updated = if (passNow) p.copy(
                exerciseLogs = logs,
                lastPassDay = td,
                lastPassIndex = p.rotationIndex.mod(p.steps.size),
                rotationIndex = (p.rotationIndex + 1).mod(p.steps.size),
            ) else p.copy(exerciseLogs = logs)
            val pl = PendingLog(planId, step.id, minutes, exerciseTotal = total, passed = total >= EXERCISE_PASS_MIN)
            result = pl
            val withPlan = d.withPlan(planId) { updated }
            (if (total >= EXERCISE_PASS_MIN) withPlan.markTaskDone(p.taskId) else withPlan).copy(pendingLog = pl)
        }
        return result
    }

    /** Ôn tập xong: nhớ tốt thì giãn lịch ra, quên nhiều thì ôn lại sau 1 ngày. */
    fun review(planId: String, stepId: String, remembered: Boolean) = update { d ->
        val td = today()
        d.withPlan(planId) { plan ->
            plan.copy(steps = plan.steps.map { s ->
                if (s.id != stepId) s
                else if (remembered) {
                    val stage = s.reviewStage + 1
                    s.copy(reviewStage = stage, nextReview = REVIEW_DAYS.getOrNull(stage)?.let { td + it })
                } else s.copy(reviewStage = 0, nextReview = td + REVIEW_DAYS.first())
            })
        }.copy(pendingLog = null)
    }

    fun setPending(p: PendingLog?) = update { it.copy(pendingLog = p) }
}
