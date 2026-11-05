package com.van.nhacviec.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.van.nhacviec.Screen
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.Task
import com.van.nhacviec.data.today
import com.van.nhacviec.timer.TimerController

@Composable
fun QuadrantScreen(d: AppData, q: Quadrant, nav: (Screen) -> Unit, back: () -> Unit) {
    val col = q.colors()
    val td = today()
    val ctx = LocalContext.current
    val mine = d.tasks.filter { it.quadrant == q }
    val open = mine.filter { !it.isDone(td) }.sortedWith(compareBy<Task>({ it.dueAt ?: Long.MAX_VALUE }, { it.createdAt }))
    val done = mine.filter { it.isDone(td) }.sortedByDescending { it.doneAt ?: 0 }

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(col.bg).statusBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundIcon("←", onClick = back)
                Column(Modifier.weight(1f)) {
                    Text("0${q.no} · ${q.label.uppercase()}", style = t(12, 800, col.ink))
                    Text(q.title, style = t(18, 800))
                }
                MochiView(Mood.HAPPY, col.mochi, 44.dp)
            }
            Text(
                when (q) {
                    Quadrant.Q1 -> "Nhắc mỗi 1 tiếng, càng hoãn càng gắt."
                    Quadrant.Q2 -> "Nhắc mỗi 3 tiếng. Việc quan trọng cho tương lai."
                    Quadrant.Q3 -> "Nhắc 1 lần mỗi ngày. Làm nhanh hoặc nhờ người khác."
                    Quadrant.Q4 -> "Nhắc nhẹ 1 lần mỗi ngày. Để sau cũng được."
                },
                style = t(13, 600, C.Muted),
            )
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(open, key = { it.id }) { task ->
                TaskRow(d, task, td,
                    onOpen = { nav(task.planId?.let { Screen.PlanDetail(it) } ?: Screen.Edit(task.id, q)) },
                    onStart = { TimerController.start(ctx, task.id); nav(Screen.Timer) },
                    onToggle = { Store.setDone(task.id, true) })
            }
            if (open.isEmpty()) {
                item { Text("Chưa có việc nào ở đây.", style = t(14, 600, C.Muted), modifier = Modifier.padding(8.dp)) }
            }
            if (done.isNotEmpty()) {
                item { Text("Đã xong", style = t(13, 800, C.Muted), modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
                items(done, key = { "d" + it.id }) { task ->
                    TaskRow(d, task, td,
                        onOpen = { nav(task.planId?.let { Screen.PlanDetail(it) } ?: Screen.Edit(task.id, q)) },
                        onStart = null,
                        onToggle = { Store.setDone(task.id, false) })
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Pill("+ Thêm việc vào mục 0${q.no}", modifier = Modifier.fillMaxWidth()) { nav(Screen.Edit(null, q)) }
        }
    }
}

@Composable
private fun TaskRow(
    d: AppData, task: Task, td: Long,
    onOpen: () -> Unit, onStart: (() -> Unit)?, onToggle: () -> Unit,
) {
    val col = task.quadrant.colors()
    val isDone = task.isDone(td)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.White).clickable(onClick = onOpen)
            .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(if (isDone) col.mochi else col.bg).clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) { if (isDone) Text("✓", style = t(16, 800, C.White)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                task.title,
                style = t(15, 800, if (isDone) C.Muted else C.Ink)
                    .copy(textDecoration = if (isDone) TextDecoration.LineThrough else null),
            )
            if (task.planId != null) {
                Text(d.detailOf(task, td), style = t(13, 600, C.Ink))
            } else {
                val dayDetail = task.todayDetail(td)
                if (dayDetail.isNotBlank()) Text("Hôm nay: $dayDetail", style = t(13, 600, C.Ink))
                if (task.detail.isNotBlank()) Text(task.detail, style = t(12, 600, C.Muted))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (task.planId != null) Tag("LỘ TRÌNH", col.bg, col.ink)
                else if (task.daily) Tag("HẰNG NGÀY", col.bg, col.ink)
                task.dueAt?.let { Tag(fmtDue(it), col.bg, col.ink) }
                val m = d.minutesToday(task.id, td)
                if (m > 0) Tag("đã làm ${fmtMinutes(m)}", C.Soft, C.Muted)
            }
        }
        if (onStart != null) {
            Box(
                Modifier.clip(RoundedCornerShape(999.dp)).background(col.bg).clickable(onClick = onStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) { Text("▶ 5'", style = t(13, 800, col.ink)) }
        }
    }
}
