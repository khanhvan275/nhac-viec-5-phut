package com.van.nhacviec.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.van.nhacviec.Screen
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.PendingLog
import com.van.nhacviec.data.PlanKind
import com.van.nhacviec.data.PlanStep
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.today
import com.van.nhacviec.timer.TimerController

@Composable
fun PlanDetailScreen(d: AppData, planId: String, nav: (Screen) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val td = today()
    val p = d.plan(planId)
    if (p == null) {
        Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().padding(24.dp)) {
            Text("Lộ trình đã bị xóa", style = t(16, 800))
            Pill("Quay lại", onClick = back)
        }
        return
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val (tagBg, tagInk) = p.kind.tagColors()
    val current = p.currentStep(td)

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(tagBg).statusBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundIcon("←", onClick = back)
                Column(Modifier.weight(1f)) {
                    Text(
                        "${p.kind.label.uppercase()} · MỤC 0${p.quadrant.no} · NHẮC ${remindText(p.quadrant)}",
                        style = t(11, 800, tagInk),
                    )
                    Text(p.name, style = t(19, 800))
                }
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(C.White)
                        .clickable { nav(Screen.ImportPlan(targetPlanId = p.id)) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) { Text("Cập nhật", style = t(12, 800, tagInk)) }
            }
            if (p.kind != PlanKind.EXERCISE) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { ProgressBar(p.overallPercent() / 100f, C.White, C.Primary, 12) }
                    Text("${p.overallPercent()}%", style = t(14, 800))
                }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (current != null) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.White)
                        .border(3.dp, Color(0xFF9EC4EC), RoundedCornerShape(22.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row {
                        Text(if (p.kind == PlanKind.EXERCISE) "HÔM NAY TẬP" else "ĐANG HỌC", style = t(11, 800, tagInk), modifier = Modifier.weight(1f))
                        if (p.kind != PlanKind.EXERCISE) Text("${current.progress}%", style = t(12, 800, tagInk))
                        else Text("${p.exerciseMinutesToday(td)} phút hôm nay", style = t(12, 800, tagInk))
                    }
                    Text(current.title, style = t(16, 800))
                    if (current.detail.isNotBlank()) Text(current.detail, style = t(12, 600, C.Muted))
                    if (p.kind != PlanKind.EXERCISE) ProgressBar(current.progress / 100f, C.Soft, Color(0xFF9EC4EC), 8)
                    Text(p.todayText(td), style = t(12, 600, C.Muted))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(
                            if (p.kind == PlanKind.EXERCISE) "▶ Tập 5 phút" else "▶ Học 5 phút",
                            size = 13, modifier = Modifier.weight(1f),
                        ) { TimerController.start(ctx, p.taskId); nav(Screen.Timer) }
                        if (p.kind != PlanKind.EXERCISE) {
                            Pill("Ghi %", bg = C.Soft, fg = C.Ink, size = 13) { Store.setPending(PendingLog(p.id, current.id, 0)) }
                        } else {
                            Pill("Ghi phút", bg = C.Soft, fg = C.Ink, size = 13) {
                                Store.setPending(PendingLog(p.id, current.id, 0, exerciseTotal = -1))
                            }
                        }
                    }
                }
            } else if (p.steps.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.White).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MochiView(Mood.HAPPY, C.Header, 56.dp)
                    Text("Đã học xong cả lộ trình! Giỏi quá!", style = t(15, 800))
                }
            }

            if (p.kind == PlanKind.EXERCISE) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.White).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Tuần này · chuỗi ${streak(p, td)} ngày", style = t(14, 800))
                    WeekStrip(p, td)
                }
                Text("Vòng lặp", style = t(13, 800, C.Muted), modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }

            p.steps.forEachIndexed { i, s ->
                StepRow(p.kind, s, i, isCurrent = s.id == current?.id, td = td, color = Color(0xFF9EC4EC)) {
                    if (p.kind != PlanKind.EXERCISE) Store.setPending(PendingLog(p.id, s.id, 0))
                }
            }

            Text("Đổi mục ưu tiên", style = t(13, 800, C.Muted), modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Quadrant.entries.forEach { q ->
                    val col = q.colors()
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(col.bg)
                            .then(if (q == p.quadrant) Modifier.border(2.dp, col.ink, RoundedCornerShape(999.dp)) else Modifier)
                            .clickable { Store.savePlan(p.copy(quadrant = q)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("0${q.no}", style = t(13, 800, col.ink)) }
                }
            }

            if (p.kind != PlanKind.EXERCISE) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Yellow).padding(14.dp),
                ) {
                    Text("Lịch ôn giãn cách", style = t(13, 800))
                    Text(
                        "Học xong một ${p.kind.stepWord} → ôn sau 1, 3, 7, 14, 30 ngày. Ôn thấy quên nhiều thì bắt đầu lại từ 1 ngày.",
                        style = t(12, 600, Color(0xFF4A3D00)),
                    )
                }
            }
            Text(
                "Xóa lộ trình này",
                style = t(14, 800, Color(0xFFA33A45)),
                modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(999.dp)).clickable { confirmDelete = true }.padding(10.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = C.White,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Xóa \"${p.name}\"?", style = t(18, 800)) },
            text = { Text("Tiến độ và lịch ôn của lộ trình này sẽ mất.", style = t(14, 600)) },
            confirmButton = {
                Pill("Xóa", bg = Color(0xFFA33A45), size = 14) { Store.deletePlan(p.id); back() }
            },
            dismissButton = { Pill("Giữ lại", bg = C.Soft, fg = C.Ink, size = 14) { confirmDelete = false } },
        )
    }
}

private fun remindText(q: Quadrant): String = when (q) {
    Quadrant.Q1 -> "MỖI 1 TIẾNG"
    Quadrant.Q2 -> "MỖI 3 TIẾNG"
    else -> "MỖI NGÀY"
}

@Composable
private fun StepRow(kind: PlanKind, s: PlanStep, index: Int, isCurrent: Boolean, td: Long, color: Color, onClick: () -> Unit) {
    val muted = kind != PlanKind.EXERCISE && !s.done && !isCurrent && s.progress == 0
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.White)
            .then(if (isCurrent) Modifier.border(2.dp, color, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp)
            .alpha(if (muted) 0.75f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (kind == PlanKind.EXERCISE) {
            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (isCurrent) ExerciseDot else C.Soft).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("${index + 1}", style = t(12, 800))
            }
        } else StepDot(s.done, color)
        Column(Modifier.weight(1f)) {
            Text(s.title, style = t(14, 700))
            val sub = buildList {
                if (s.minutes > 0) add("${s.minutes} phút")
                if (kind != PlanKind.EXERCISE && !s.done && s.progress > 0) add("đã ${s.progress}%")
                if (isCurrent && kind == PlanKind.EXERCISE) add("hôm nay")
                s.nextReview?.let {
                    add(if (it <= td) "ôn hôm nay · lần ${s.reviewStage + 1}" else "ôn lần tới: ${fmtDayShort(it)}")
                }
                if (s.done && s.nextReview == null && s.reviewStage > 0) add("đã nhớ lâu")
            }.joinToString(" · ")
            if (sub.isNotBlank()) {
                val reviewToday = s.nextReview?.let { it <= td } == true
                Text(sub, style = t(11, if (reviewToday) 700 else 600, if (reviewToday) Color(0xFF7D5E00) else C.Muted))
            }
            if (s.detail.isNotBlank()) Text(s.detail, style = t(11, 600, C.Muted), maxLines = 2)
        }
    }
}

fun fmtDayShort(epochDay: Long): String {
    val d = java.time.LocalDate.ofEpochDay(epochDay)
    return "${d.dayOfMonth}/${d.monthValue}"
}
