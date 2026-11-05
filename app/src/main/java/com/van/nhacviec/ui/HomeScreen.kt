package com.van.nhacviec.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.van.nhacviec.Screen
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Task
import com.van.nhacviec.data.today
import com.van.nhacviec.update.Updater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(d: AppData, nav: (Screen) -> Unit, tab: (Screen) -> Unit) {
    val td = today()
    val relevant = d.tasks.filter { it.daily || it.doneAt == null || isToday(it.doneAt) }
    val doneCount = relevant.count { it.isDone(td) }
    val total = relevant.size
    val minutes = d.sessions.filter { it.day == td }.sumOf { it.minutes }
    val pct = if (total == 0) 0 else doneCount * 100 / total

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Dải xanh trên cùng
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(C.Header).statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(todayLabel(), style = t(13, 600, Color(0xFF2F4A70)))
                    Text("Hi, ${d.settings.name}!", style = t(26, 800))
                }
                MochiView(if (pct >= 50) Mood.HAPPY else Mood.CALM, C.White, 54.dp)
            }
            // Thẻ tiến độ
            Row(
                Modifier.padding(horizontal = 16.dp).offset(y = (-40).dp).fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp)).background(C.White).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier.size(66.dp).clip(CircleShape).background(C.Soft).border(5.dp, Color(0xFF9EC4EC), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("$pct%", style = t(18, 800, Color(0xFF2F5A92))) }
                Column {
                    Text("Kế hoạch hôm nay", style = t(16, 800))
                    Text("$doneCount / $total việc xong · đã làm ${fmtMinutes(minutes)}", style = t(13, 600, C.Muted))
                }
            }
            Column(Modifier.offset(y = (-28).dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UpdateBanner()
                RunningTimerBar(d) { nav(Screen.Timer) }
                val reviews = d.dueReviews(td)
                if (reviews.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Yellow)
                            .clickable { tab(Screen.PlansTab) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MochiView(Mood.PLEAD, C.White, 34.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Hôm nay ôn ${reviews.size} bài", style = t(14, 800))
                            Text(reviews.take(2).joinToString(" · ") { it.second.title }, style = t(12, 600, C.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("Ôn ▶", style = t(13, 800, Color(0xFF7D5E00)))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuadrantCard(d, Quadrant.Q1, Modifier.weight(1f)) { nav(Screen.InQuadrant(Quadrant.Q1)) }
                    QuadrantCard(d, Quadrant.Q2, Modifier.weight(1f)) { nav(Screen.InQuadrant(Quadrant.Q2)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuadrantCard(d, Quadrant.Q3, Modifier.weight(1f)) { nav(Screen.InQuadrant(Quadrant.Q3)) }
                    QuadrantCard(d, Quadrant.Q4, Modifier.weight(1f)) { nav(Screen.InQuadrant(Quadrant.Q4)) }
                }
                if (d.tasks.isEmpty()) {
                    Text(
                        "Bấm + để thêm việc đầu tiên. Việc hằng ngày như tiếng Anh, thể dục chỉ cần thêm một lần.",
                        style = t(13, 600, C.Muted),
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        }
        BottomBar(Screen.Home, tab) { nav(Screen.Edit(null, Quadrant.Q1)) }
    }
}

private fun isToday(ms: Long?): Boolean {
    ms ?: return false
    return java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        .toEpochDay() == today()
}

@Composable
private fun QuadrantCard(d: AppData, q: Quadrant, modifier: Modifier, onClick: () -> Unit) {
    val col = q.colors()
    val td = today()
    val mine = d.tasks.filter { it.quadrant == q }
    val open = mine.filter { !it.isDone(td) }.sortedWith(compareBy<Task>({ it.dueAt ?: Long.MAX_VALUE }, { it.createdAt }))
    val doneToday = mine.filter { it.isDone(td) && (it.daily || isToday(it.doneAt)) }
    Column(
        modifier.heightIn(min = 170.dp).clip(RoundedCornerShape(24.dp)).background(col.bg)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("0${q.no} ${q.label}", style = t(13, 800, col.ink), modifier = Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(C.White).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("${open.size}", style = t(12, 800, col.ink))
            }
        }
        open.take(3).forEach { task ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.White).padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(task.title, style = t(13, 700), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = d.detailOf(task, td)
                if (sub.isNotBlank()) Text(sub, style = t(11, 600, C.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (task.planId != null) Text("LỘ TRÌNH", style = t(10, 800, col.ink))
                else if (task.daily) Text("HẰNG NGÀY", style = t(10, 800, col.ink))
                else task.dueAt?.let { Text(fmtDue(it), style = t(11, 700, col.ink)) }
            }
        }
        if (open.size > 3) Text("+${open.size - 3} việc nữa", style = t(12, 700, col.ink))
        doneToday.take(2).forEach {
            Text(
                "${it.title} · ${fmtMinutes(d.minutesToday(it.id, td))}",
                style = t(12, 600, C.Muted).copy(textDecoration = TextDecoration.LineThrough),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (mine.isEmpty()) Text(q.title, style = t(12, 600, col.ink))
    }
}

@Composable
fun RunningTimerBar(d: AppData, onClick: () -> Unit) {
    val tm = d.timer ?: return
    val task = d.task(tm.taskId) ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tm) {
        while (true) {
            now = System.currentTimeMillis(); delay(1000)
        }
    }
    val sec = tm.elapsedMs(now) / 1000
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(task.quadrant.colors().bg)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MochiView(if (tm.runningSince != null) Mood.HAPPY else Mood.CALM, task.quadrant.colors().mochi, 32.dp)
        Column(Modifier.weight(1f)) {
            Text(if (tm.runningSince != null) "Đang làm" else "Tạm dừng", style = t(11, 700, C.Muted))
            Text(d.titleOf(task), style = t(14, 800), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("%02d:%02d".format(sec / 60, sec % 60), style = t(18, 800, task.quadrant.colors().ink))
    }
}

@Composable
fun UpdateBanner() {
    val st by Updater.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    when (val s = st) {
        is Updater.State.Available -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Yellow).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Có bản mới!", style = t(14, 800))
                Text(s.r.notes.lineSequence().firstOrNull()?.take(60) ?: s.r.name, style = t(12, 600, C.Muted), maxLines = 2)
            }
            Spacer(Modifier.size(8.dp))
            Pill("Cập nhật", size = 13) { scope.launch { Updater.downloadAndInstall(ctx, s.r) } }
        }
        is Updater.State.Downloading -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Yellow).padding(14.dp)
        ) { Text("Đang tải bản mới... ${s.percent}%", style = t(14, 800)) }
        else -> {}
    }
}
