package com.van.nhacviec.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.van.nhacviec.data.Settings
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.today
import com.van.nhacviec.widget.updateAllWidgets
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val WORK = "nhac_viec_dinh_ky"
    private const val MAX_PER_RUN = 2

    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun inSleep(s: Settings, minute: Int = LocalTime.now().let { it.hour * 60 + it.minute }): Boolean =
        if (s.sleepStart > s.sleepEnd) minute >= s.sleepStart || minute < s.sleepEnd
        else minute in s.sleepStart until s.sleepEnd

    /** Chạy mỗi ~15 phút: gửi lời nhắc đến hạn và thông báo đếm ngày. */
    fun run(ctx: Context) {
        val d = Store.value
        if (inSleep(d.settings)) return
        val now = System.currentTimeMillis()
        val td = today()

        // Đếm ngày: chỉ báo trước 5 ngày và 1 ngày.
        d.countdowns.forEach { c ->
            val left = c.daysLeft(td)
            val mark = left.toInt()
            if ((mark == 5 || mark == 1) && mark !in c.notified) {
                Notifier.countdown(ctx, c, left)
                Store.upsertCountdown(c.copy(notified = c.notified + mark))
            }
        }

        // Ôn tập giãn cách: báo một lần mỗi ngày.
        val reviews = d.dueReviews(td)
        if (reviews.isNotEmpty()) {
            val fresh = reviews.filter { it.first.reviewNotifiedDay != td }
            if (fresh.isNotEmpty()) {
                Notifier.review(ctx, reviews)
                fresh.map { it.first }.distinctBy { it.id }.forEach { p ->
                    Store.update { x -> x.copy(plans = x.plans.map { if (it.id == p.id) it.copy(reviewNotifiedDay = td) else it }) }
                }
            }
        }

        // Đang bấm giờ thì không làm phiền.
        if (d.timer?.runningSince != null) return

        var sent = 0
        val due = d.tasks
            .filter { !it.isDone(td) }
            // Lộ trình đã học xong hết thì thôi nhắc.
            .filter { t -> d.planOf(t)?.let { it.currentStep(td) != null } ?: true }
            .map { t -> if (t.daily && t.nagDay != td) t.copy(nagLevel = 0, lastRemindAt = 0, nagDay = td) else t }
            .filter { t ->
                val since = if (t.lastRemindAt == 0L && !t.daily) t.createdAt else t.lastRemindAt
                now - since >= t.quadrant.remindMinutes * 60_000L
            }
            .sortedWith(compareBy<com.van.nhacviec.data.Task>({ it.quadrant.no }, { it.dueAt ?: Long.MAX_VALUE }))

        for (t in due) {
            if (sent >= MAX_PER_RUN) break
            val level = t.nagLevel.coerceAtMost(t.quadrant.maxLevel)
            Notifier.nag(ctx, t, level)
            Store.upsertTask(
                t.copy(
                    lastRemindAt = now,
                    nagLevel = (level + 1).coerceAtMost(t.quadrant.maxLevel),
                    nagDay = td,
                )
            )
            sent++
        }
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        runCatching { Reminders.run(applicationContext) }
        updateAllWidgets(applicationContext)
        return Result.success()
    }
}
