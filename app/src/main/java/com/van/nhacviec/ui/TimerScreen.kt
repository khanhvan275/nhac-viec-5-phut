package com.van.nhacviec.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.van.nhacviec.data.AppData
import com.van.nhacviec.timer.TimerController
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(d: AppData, back: () -> Unit) {
    val ctx = LocalContext.current
    val tm = d.timer
    val task = tm?.let { d.task(it.taskId) }
    if (tm == null || task == null) {
        Column(
            Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MochiView(Mood.CALM, C.Header, 90.dp)
            Text("Không có việc nào đang bấm giờ", style = t(16, 800))
            Pill("Về trang chính", onClick = back)
        }
        return
    }
    val col = task.quadrant.colors()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tm) {
        while (true) {
            now = System.currentTimeMillis(); delay(500)
        }
    }
    val elapsed = tm.elapsedMs(now)
    val sec = elapsed / 1000
    val target = tm.targetMin * 60_000L
    val progress = (elapsed.toFloat() / target).coerceIn(0f, 1f)
    val running = tm.runningSince != null
    val reached = elapsed >= target && tm.targetMin < 24 * 60
    val unlimited = tm.targetMin >= 24 * 60

    Column(Modifier.fillMaxSize().background(C.Header).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoundIcon("←", bg = Color(0xB3FFFFFF), onClick = back)
            Column {
                Text("0${task.quadrant.no} · ${task.quadrant.label.uppercase()}", style = t(12, 800, Color(0xFF2F4A70)))
                Text(task.todayDetail().ifBlank { task.title }, style = t(17, 800))
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)).background(C.White)
                .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 18.dp.toPx()
                    drawArc(C.Soft, 0f, 360f, false, style = Stroke(sw))
                    drawArc(
                        if (unlimited) col.mochi else Color(0xFF8FB8E6),
                        -90f, if (unlimited) 360f else 360f * progress, false,
                        style = Stroke(sw, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MochiView(if (running) Mood.HAPPY else Mood.CALM, col.mochi, 44.dp)
                    Text("%02d:%02d".format(sec / 60, sec % 60), style = t(50, 800, Color(0xFF2F5A92)))
                    Text(if (unlimited) "làm tiếp thoải mái" else "mục tiêu ${tm.targetMin} phút", style = t(13, 600, C.Muted))
                }
            }
            Text(
                when {
                    !running -> "Đang tạm dừng. Nghỉ chút rồi quay lại nha."
                    reached -> "${tm.targetMin} phút rồi! Giỏi quá. Làm tiếp hay nghỉ?"
                    else -> "Đang ổn đấy! Keep going."
                },
                style = t(14, 600), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Soft).padding(14.dp),
            )
            if (reached && running) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Pill("+10 phút", bg = C.Yellow, fg = C.Ink, size = 14, modifier = Modifier.weight(1f)) { TimerController.extend(ctx, 10) }
                    Pill("Làm tiếp", bg = C.Yellow, fg = C.Ink, size = 14, modifier = Modifier.weight(1f)) { TimerController.keepGoing(ctx) }
                    Pill("Nghỉ", bg = C.Yellow, fg = C.Ink, size = 14, modifier = Modifier.weight(1f)) {
                        TimerController.finish(ctx, markDone = false); back()
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Pill(if (running) "Tạm dừng" else "Tiếp tục", bg = C.Soft, fg = C.Ink, modifier = Modifier.weight(1f)) {
                    if (running) TimerController.pause(ctx) else TimerController.resume(ctx)
                }
                Pill("Xong việc", modifier = Modifier.weight(1f)) {
                    TimerController.finish(ctx, markDone = true); back()
                }
            }
            if (!reached) {
                Text(
                    "Dừng, chưa xong",
                    style = t(13, 700, C.Muted),
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .clickable { TimerController.finish(ctx, markDone = false); back() }
                        .padding(10.dp),
                )
            }
            Text("Đồng hồ vẫn chạy trên thanh thông báo khi Vân rời app", style = t(12, 600, C.Muted), textAlign = TextAlign.Center)
        }
    }
}
