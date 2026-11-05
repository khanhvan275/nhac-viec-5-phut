package com.van.nhacviec.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.van.nhacviec.R
import com.van.nhacviec.data.EXERCISE_GREAT_MIN
import com.van.nhacviec.data.EXERCISE_PASS_MIN
import com.van.nhacviec.data.PendingLog
import com.van.nhacviec.data.Plan
import com.van.nhacviec.data.PlanKind
import com.van.nhacviec.data.Session
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.TimerState
import com.van.nhacviec.data.today
import com.van.nhacviec.notify.ActionReceiver
import com.van.nhacviec.notify.Notifier
import com.van.nhacviec.ui.C
import com.van.nhacviec.ui.Mochi
import com.van.nhacviec.ui.Mood
import com.van.nhacviec.ui.colors

/** Bấm giờ: đếm thời gian làm việc, hết mục tiêu thì hỏi làm tiếp hay nghỉ. */
object TimerController {

    fun start(ctx: Context, taskId: String, reviewStepId: String? = null) {
        val cur = Store.value.timer
        if (cur != null && (cur.taskId != taskId || cur.reviewStepId != reviewStepId)) finish(ctx, markDone = false)
        if (Store.value.timer?.taskId == taskId) {
            resume(ctx)
            return
        }
        val now = System.currentTimeMillis()
        Store.update { it.copy(timer = TimerState(taskId = taskId, runningSince = now, reviewStepId = reviewStepId)) }
        Notifier.cancel(ctx, taskId.hashCode())
        refresh(ctx)
    }

    fun pause(ctx: Context) {
        val t = Store.value.timer ?: return
        val since = t.runningSince ?: return
        val now = System.currentTimeMillis()
        Store.update { it.copy(timer = t.copy(runningSince = null, accumulatedMs = t.accumulatedMs + (now - since))) }
        refresh(ctx)
    }

    fun resume(ctx: Context) {
        val t = Store.value.timer ?: return
        if (t.runningSince != null) return
        Store.update { it.copy(timer = t.copy(runningSince = System.currentTimeMillis())) }
        refresh(ctx)
    }

    fun extend(ctx: Context, minutes: Int) {
        val t = Store.value.timer ?: return
        val elapsedMin = (t.elapsedMs() / 60_000).toInt()
        val newTarget = maxOf(t.targetMin, elapsedMin) + minutes
        Store.update { it.copy(timer = t.copy(targetMin = newTarget, runningSince = t.runningSince ?: System.currentTimeMillis(), accumulatedMs = t.accumulatedMs)) }
        Notifier.cancel(ctx, Notifier.ID_TIMER_PROMPT)
        refresh(ctx)
    }

    /** Bỏ qua câu hỏi, cứ làm tiếp không giới hạn. */
    fun keepGoing(ctx: Context) {
        val t = Store.value.timer ?: return
        Store.update { it.copy(timer = t.copy(targetMin = 24 * 60)) }
        Notifier.cancel(ctx, Notifier.ID_TIMER_PROMPT)
        refresh(ctx)
    }

    fun finish(ctx: Context, markDone: Boolean) {
        val t = Store.value.timer ?: return
        val ms = t.elapsedMs()
        val minutes = ((ms + 30_000) / 60_000).toInt()
        Store.update { d ->
            val sessions = if (minutes > 0) d.sessions + Session(t.taskId, t.startedAt, minutes, today()) else d.sessions
            d.copy(timer = null, sessions = sessions)
        }
        val plan = Store.value.planOf(Store.value.task(t.taskId))
        if (plan != null) {
            finishPlan(ctx, plan, t, minutes)
        } else if (markDone) Store.setDone(t.taskId, true) else Store.snooze(t.taskId)
        cancelAlarm(ctx)
        Notifier.cancel(ctx, Notifier.ID_TIMER)
        Notifier.cancel(ctx, Notifier.ID_TIMER_PROMPT)
    }

    /** Việc của lộ trình: hỏi % đã học, ghi phút tập, hoặc hỏi kết quả ôn tập. */
    private fun finishPlan(ctx: Context, plan: Plan, t: TimerState, minutes: Int) {
        val title: String
        val body: String
        if (t.reviewStepId != null) {
            Store.setPending(PendingLog(plan.id, t.reviewStepId, minutes, review = true))
            title = "Ôn xong rồi! Nhớ được nhiều không?"
            body = "Mở app để chọn Nhớ tốt hoặc Quên nhiều."
        } else if (plan.kind == PlanKind.EXERCISE) {
            val pl = Store.logExercise(plan.id, minutes)
            val total = pl?.exerciseTotal ?: minutes
            title = when {
                total >= EXERCISE_GREAT_MIN -> "$total phút! Amazing, vượt mức rồi!"
                total >= EXERCISE_PASS_MIN -> "Đạt rồi! $total phút hôm nay"
                else -> "Mới $total phút, thêm chút nữa là đạt"
            }
            body = if (total >= EXERCISE_PASS_MIN) "Mai: ${Store.value.plan(plan.id)?.currentStep(today() + 1)?.title ?: ""}. Chỉ cần 5 phút."
            else "Cần $EXERCISE_PASS_MIN phút là đạt hôm nay."
        } else {
            val step = plan.currentStep() ?: return
            Store.setPending(PendingLog(plan.id, step.id, minutes))
            title = "Học được $minutes phút, giỏi lắm!"
            body = "${step.title} đã xong đến bao nhiêu %? Chạm để ghi lại."
        }
        val n = NotificationCompat.Builder(ctx, Notifier.CH_NAG)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(C.Primary.toArgb())
            .setLargeIcon(Mochi.bitmap(192, Mood.HAPPY, 0xFFB9D7F4.toInt()))
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(Notifier.openAppIntent(ctx, null, 9))
            .setAutoCancel(true)
            .build()
        Notifier.notify(ctx, Notifier.ID_LOG, n)
    }

    fun restore(ctx: Context) = refresh(ctx)

    /** Cập nhật thông báo đang chạy và hẹn giờ hỏi. */
    fun refresh(ctx: Context) {
        val t = Store.value.timer
        if (t == null) {
            Notifier.cancel(ctx, Notifier.ID_TIMER)
            cancelAlarm(ctx)
            return
        }
        val task = Store.value.task(t.taskId)
        val title = task?.let { Store.value.titleOf(it) } ?: "Đang làm việc"
        val running = t.runningSince != null
        val color = task?.quadrant?.colors()?.mochi?.toArgb() ?: 0xFFB9D7F4.toInt()
        val b = NotificationCompat.Builder(ctx, Notifier.CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(C.Primary.toArgb())
            .setLargeIcon(Mochi.bitmap(160, if (running) Mood.HAPPY else Mood.CALM, color))
            .setContentTitle(if (running) "Đang làm: $title" else "Tạm dừng: $title")
            .setContentText("Mục tiêu ${t.targetMin} phút")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(Notifier.openAppIntent(ctx, null, 7))
        if (running) {
            b.setUsesChronometer(true).setWhen(System.currentTimeMillis() - t.elapsedMs()).setShowWhen(true)
            b.addAction(0, "Tạm dừng", Notifier.action(ctx, ActionReceiver.T_PAUSE, null, 11))
        } else {
            b.setShowWhen(false)
            b.addAction(0, "Tiếp tục", Notifier.action(ctx, ActionReceiver.T_RESUME, null, 12))
        }
        b.addAction(0, "Xong việc", Notifier.action(ctx, ActionReceiver.T_DONE, null, 13))
        Notifier.notify(ctx, Notifier.ID_TIMER, b.build())

        if (running) {
            val remain = t.targetMin * 60_000L - t.elapsedMs()
            if (remain > 0) scheduleAlarm(ctx, System.currentTimeMillis() + remain) else cancelAlarm(ctx)
        } else {
            cancelAlarm(ctx)
        }
    }

    fun showPrompt(ctx: Context) {
        val t = Store.value.timer ?: return
        if (t.runningSince == null) return
        val title = Store.value.task(t.taskId)?.title ?: ""
        val n = NotificationCompat.Builder(ctx, Notifier.CH_NAG)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(C.Primary.toArgb())
            .setLargeIcon(Mochi.bitmap(192, Mood.HAPPY, 0xFFB9D7F4.toInt()))
            .setContentTitle("${t.targetMin} phút rồi! Giỏi quá")
            .setContentText("$title — làm tiếp hay nghỉ? Keep going?")
            .setContentIntent(Notifier.openAppIntent(ctx, null, 8))
            .addAction(0, "+10 phút", Notifier.action(ctx, ActionReceiver.T_PLUS10, null, 21))
            .addAction(0, "Làm tiếp", Notifier.action(ctx, ActionReceiver.T_KEEP, null, 22))
            .addAction(0, "Nghỉ", Notifier.action(ctx, ActionReceiver.T_STOP, null, 23))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        Notifier.notify(ctx, Notifier.ID_TIMER_PROMPT, n)
    }

    private fun alarmIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, ActionReceiver::class.java).apply { action = ActionReceiver.T_PROMPT }
        return PendingIntent.getBroadcast(ctx, 99, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun scheduleAlarm(ctx: Context, at: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(ctx)
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAlarm(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(alarmIntent(ctx))
    }
}
