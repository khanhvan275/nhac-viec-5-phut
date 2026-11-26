package com.van.nhacviec.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.van.nhacviec.MainActivity
import com.van.nhacviec.R
import com.van.nhacviec.data.Countdown
import com.van.nhacviec.data.Plan
import com.van.nhacviec.data.PlanStep
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.Task
import com.van.nhacviec.ui.C
import com.van.nhacviec.ui.Mochi
import com.van.nhacviec.ui.Mood
import com.van.nhacviec.ui.colors

object Notifier {
    const val CH_NAG = "nhac_viec"
    const val CH_TIMER = "bam_gio"
    const val CH_COUNT = "dem_ngay"

    const val ID_TIMER = 1001
    const val ID_TIMER_PROMPT = 1002
    const val ID_LOG = 1003
    const val ID_REVIEW = 1004

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_NAG, "Nhắc việc", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Lời nhắc làm việc 5 phút" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMER, "Bấm giờ", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_COUNT, "Đếm ngày", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun openAppIntent(ctx: Context, startTaskId: String? = null, req: Int = 0, openCountdown: Boolean = false): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (startTaskId != null) putExtra(MainActivity.EXTRA_START_TASK, startTaskId)
            if (openCountdown) putExtra(MainActivity.EXTRA_OPEN_QUADRANT, "countdown")
        }
        return PendingIntent.getActivity(ctx, req, i, flags())
    }

    fun action(ctx: Context, action: String, taskId: String?, req: Int): PendingIntent {
        val i = Intent(ctx, ActionReceiver::class.java).apply {
            this.action = action
            putExtra(ActionReceiver.EXTRA_TASK, taskId)
        }
        return PendingIntent.getBroadcast(ctx, req, i, flags())
    }

    fun nag(ctx: Context, task: Task, level: Int) {
        if (!canNotify(ctx)) return
        val line = Messages.pick(level, task.title)
        val detail = Store.value.detailOf(task)
        val body = if (detail.isNotBlank()) "${line.body}\n${detail}" else line.body
        val icon = Mochi.bitmap(192, Messages.mood(level), task.quadrant.colors().mochi.toArgb())
        val nid = task.id.hashCode()
        val n = NotificationCompat.Builder(ctx, CH_NAG)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(C.Primary.toArgb())
            .setLargeIcon(icon)
            .setContentTitle(line.title)
            .setContentText(line.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(ctx, null, nid))
            .addAction(0, "▶ Bắt đầu 5 phút", openAppIntent(ctx, task.id, nid + 1))
            .addAction(0, "Nhắc lại sau", action(ctx, ActionReceiver.SNOOZE, task.id, nid + 2))
            .addAction(0, "Xong rồi", action(ctx, ActionReceiver.DONE, task.id, nid + 3))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notify(ctx, nid, n)
    }

    fun countdown(ctx: Context, c: Countdown, days: Long) {
        if (!canNotify(ctx)) return
        val renew = c.renews()
        val title = when {
            days <= 0 && renew -> "${c.name} hết chu trình hôm nay"
            days <= 0 -> "Hôm nay là hạn cuối ${c.name}!"
            days <= 1 -> "Mai là hết hạn ${c.name}!"
            else -> "Còn $days ngày ${c.name}"
        }
        val body = when {
            days <= 0 && renew -> "Không chọn gì thì mai app tự chạy tiếp ${c.cycleLabel()}. Muốn dừng thì bấm Dừng."
            c.contest -> "Chuẩn bị bài thi nhé. You can do it!"
            renew -> "Tận dụng nốt nha: dùng ${c.name} 30 phút hôm nay? Hết hạn app sẽ tự chạy chu trình mới."
            else -> "Tận dụng nốt nha: dùng ${c.name} 30 phút hôm nay?"
        }
        val nid = c.id.hashCode() + days.toInt()
        val b = NotificationCompat.Builder(ctx, CH_COUNT)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(C.Primary.toArgb())
            .setLargeIcon(Mochi.bitmap(192, Mood.PLEAD, 0xFFFBDADB.toInt()))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(ctx, null, nid, openCountdown = true))
            .setAutoCancel(true)
        if (days <= 0 && renew) {
            b.addAction(0, if (c.isMonthly()) "Tiếp 1 tháng" else "Lặp lại ${c.cycleLabel()}",
                countdownAction(ctx, ActionReceiver.CD_SAME, c.id, nid, 1))
            if (!c.isMonthly()) b.addAction(0, "Tiếp 1 tháng", countdownAction(ctx, ActionReceiver.CD_MONTH, c.id, nid, 2))
            b.addAction(0, "Dừng", countdownAction(ctx, ActionReceiver.CD_STOP, c.id, nid, 3))
        }
        notify(ctx, nid, b.build())
    }

    private fun countdownAction(ctx: Context, action: String, id: String, nid: Int, k: Int): PendingIntent {
        val i = Intent(ctx, ActionReceiver::class.java).apply {
            this.action = action
            putExtra(ActionReceiver.EXTRA_COUNTDOWN, id)
            putExtra(ActionReceiver.EXTRA_NOTIF, nid)
        }
        return PendingIntent.getBroadcast(ctx, nid * 4 + k, i, flags())
    }

    fun review(ctx: Context, items: List<Pair<Plan, PlanStep>>) {
        if (!canNotify(ctx) || items.isEmpty()) return
        val first = items.first()
        val title = if (items.size == 1) "Hôm nay ôn: ${first.second.title}" else "Hôm nay có ${items.size} bài cần ôn"
        val body = items.take(4).joinToString("\n") { "• ${it.second.title} · ${it.first.name}" } +
            "\nÔn lại một chút để nhớ lâu hơn. Just 5 minutes!"
        val n = NotificationCompat.Builder(ctx, CH_NAG)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(C.Primary.toArgb())
            .setLargeIcon(Mochi.bitmap(192, Mood.PLEAD, 0xFFFCEFC0.toInt()))
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(ctx, null, ID_REVIEW))
            .setAutoCancel(true)
            .build()
        notify(ctx, ID_REVIEW, n)
    }

    fun notify(ctx: Context, id: Int, n: android.app.Notification) {
        if (!canNotify(ctx)) return
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)
}
