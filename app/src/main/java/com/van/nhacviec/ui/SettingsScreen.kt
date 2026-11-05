package com.van.nhacviec.ui

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.van.nhacviec.BuildConfig
import com.van.nhacviec.Screen
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.Task
import com.van.nhacviec.notify.Notifier
import com.van.nhacviec.update.Updater
import kotlinx.coroutines.launch

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(d: AppData, tab: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by Updater.state.collectAsState()
    val s = d.settings

    fun pickTime(current: Int, set: (Int) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> set(h * 60 + m) }, current / 60, current % 60, true).show()
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Cài đặt", style = t(24, 800))

            Section("Giờ ngủ (không nhắc)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Từ ${fmtClock(s.sleepStart)}", bg = C.Soft, fg = C.Ink, size = 14, modifier = Modifier.weight(1f)) {
                        pickTime(s.sleepStart) { Store.setSettings(Store.value.settings.copy(sleepStart = it)) }
                    }
                    Pill("Đến ${fmtClock(s.sleepEnd)}", bg = C.Soft, fg = C.Ink, size = 14, modifier = Modifier.weight(1f)) {
                        pickTime(s.sleepEnd) { Store.setSettings(Store.value.settings.copy(sleepEnd = it)) }
                    }
                }
            }

            Section("Để nhắc nhở chạy đều") {
                Text(
                    "Điện thoại hay tự tắt app chạy ngầm để tiết kiệm pin. Bấm nút dưới và chọn Cho phép.",
                    style = t(13, 600, C.Muted),
                )
                val pm = ctx.getSystemService(PowerManager::class.java)
                val ignoring = pm.isIgnoringBatteryOptimizations(ctx.packageName)
                Pill(if (ignoring) "Đã tắt tiết kiệm pin ✓" else "Tắt tiết kiệm pin cho app", modifier = Modifier.fillMaxWidth(),
                    bg = if (ignoring) C.Green else C.Primary, fg = if (ignoring) C.Ink else C.White, size = 14) {
                    if (!ignoring) runCatching {
                        ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                    }
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    val am = ctx.getSystemService(android.app.AlarmManager::class.java)
                    if (!am.canScheduleExactAlarms()) {
                        Pill("Cho phép hẹn giờ chính xác", bg = C.Soft, fg = C.Ink, size = 14, modifier = Modifier.fillMaxWidth()) {
                            runCatching {
                                ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                            }
                        }
                    }
                }
                Pill("Thử một lời nhắc", bg = C.Soft, fg = C.Ink, size = 14, modifier = Modifier.fillMaxWidth()) {
                    val sample = d.tasks.firstOrNull { !it.isDone() } ?: Task(title = "Tiếng Anh", quadrant = Quadrant.Q2)
                    Notifier.nag(ctx, sample, (0..3).random())
                }
            }

            Section("Cập nhật app") {
                Text("Bản hiện tại: ${BuildConfig.VERSION_NAME}", style = t(14, 700))
                Text(
                    when (val x = st) {
                        Updater.State.Checking -> "Đang kiểm tra..."
                        Updater.State.UpToDate -> "Đang dùng bản mới nhất."
                        is Updater.State.Available -> "Có bản mới: ${x.r.name}"
                        is Updater.State.Downloading -> "Đang tải... ${x.percent}%"
                        is Updater.State.Failed -> x.msg
                        Updater.State.Idle -> "App tự kiểm tra bản mới mỗi lần mở."
                    },
                    style = t(13, 600, C.Muted),
                )
                val avail = st as? Updater.State.Available
                if (avail != null) {
                    if (avail.r.notes.isNotBlank()) Text(avail.r.notes, style = t(13, 600))
                    Pill("Cập nhật ngay", modifier = Modifier.fillMaxWidth(), size = 14) {
                        scope.launch { Updater.downloadAndInstall(ctx, avail.r) }
                    }
                } else {
                    Pill("Kiểm tra bản mới", bg = C.Soft, fg = C.Ink, modifier = Modifier.fillMaxWidth(), size = 14) {
                        scope.launch { Updater.check(force = true) }
                    }
                }
            }

            Section("Sắp có") {
                Text("AI tự xếp việc, nhật ký cuối ngày (hỏi lúc ${fmtClock(s.journalMinute)}) và review cuối tháng sẽ có ở bản tiếp theo.", style = t(13, 600, C.Muted))
            }
        }
        BottomBar(Screen.SettingsTab, tab)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.White).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = t(15, 800))
        content()
    }
}
