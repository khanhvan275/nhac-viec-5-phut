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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.van.nhacviec.Screen
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.EXERCISE_GREAT_MIN
import com.van.nhacviec.data.EXERCISE_PASS_MIN
import com.van.nhacviec.data.Plan
import com.van.nhacviec.data.PlanKind
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.today
import com.van.nhacviec.timer.TimerController
import java.time.LocalDate

val ExerciseGreen = Color(0xFFCDEBD8)
val ExerciseInk = Color(0xFF2F6B45)
val ExerciseDot = Color(0xFF9ED3B2)

fun PlanKind.tagColors(): Pair<Color, Color> = when (this) {
    PlanKind.BOOK -> Color(0xFFCFE3F7) to Color(0xFF2F5A92)
    PlanKind.EXERCISE -> ExerciseGreen to ExerciseInk
    PlanKind.COURSE -> Color(0xFFE7DFF6) to Color(0xFF5B4787)
}

@Composable
fun PlansScreen(d: AppData, nav: (Screen) -> Unit, tab: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val td = today()
    val reviews = d.dueReviews(td)

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(C.Header).statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Học, tập, khóa học", style = t(13, 600, Color(0xFF2F4A70)))
                    Text("Lộ trình", style = t(24, 800))
                }
                Pill("+ Nhập lộ trình", size = 13) { nav(Screen.ImportPlan()) }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (reviews.isNotEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Yellow).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MochiView(Mood.PLEAD, C.White, 30.dp)
                            Text("Hôm nay ôn tập", style = t(14, 800), modifier = Modifier.weight(1f))
                            Text("ôn giãn cách", style = t(12, 700, Color(0xFF6B5100)))
                        }
                        reviews.forEach { (plan, step) ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.White)
                                    .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("${step.title} · ${plan.name}", style = t(13, 700), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("lần ôn thứ ${step.reviewStage + 1}", style = t(11, 600, C.Muted))
                                }
                                Box(
                                    Modifier.clip(RoundedCornerShape(999.dp)).background(C.Soft)
                                        .clickable { Store.review(plan.id, step.id, remembered = true) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) { Text("Nhớ ✓", style = t(12, 800, Color(0xFF2F5A92))) }
                                Box(
                                    Modifier.clip(RoundedCornerShape(999.dp)).background(C.Primary)
                                        .clickable { TimerController.start(ctx, plan.taskId, reviewStepId = step.id); nav(Screen.Timer) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) { Text("Ôn ▶", style = t(12, 800, C.White)) }
                            }
                        }
                    }
                }
                if (d.plans.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.White).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MochiView(Mood.HAPPY, C.Header, 64.dp)
                        Text("Chưa có lộ trình nào", style = t(15, 800))
                        Text(
                            "Nhờ ChatGPT/Gemini soạn lộ trình học hay tập, rồi bấm Chia sẻ → Nhắc Việc 5 Phút, hoặc dán vào đây.",
                            style = t(13, 600, C.Muted), textAlign = TextAlign.Center,
                        )
                    }
                }
                d.plans.sortedBy { it.createdAt }.forEach { p -> PlanCard(p, td) { nav(Screen.PlanDetail(p.id)) } }
            }
        }
        BottomBar(Screen.PlansTab, tab)
    }
}

@Composable
private fun PlanCard(p: Plan, td: Long, onClick: () -> Unit) {
    val (tagBg, tagInk) = p.kind.tagColors()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.White).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val tag = if (p.kind == PlanKind.EXERCISE) "TẬP LUYỆN · XOAY VÒNG" else "${p.kind.label.uppercase()} · 0${p.quadrant.no}"
            Tag(tag, tagBg, tagInk)
            Spacer1()
            Text(
                if (p.kind == PlanKind.EXERCISE) "chuỗi ${streak(p, td)} ngày" else "${p.doneCount()} / ${p.steps.size} ${p.kind.stepWord}",
                style = t(12, 700, C.Muted),
            )
        }
        Text(p.name, style = t(16, 800))
        Text(p.todayText(td), style = t(13, 600, Color(0xFF3A4A66)))
        if (p.kind == PlanKind.EXERCISE) WeekStrip(p, td)
        else ProgressBar(p.overallPercent() / 100f, tagBg.copy(alpha = 1f), Color(0xFF9EC4EC))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Spacer1() {
    Box(Modifier.weight(1f))
}

@Composable
fun ProgressBar(fraction: Float, track: Color, fill: Color, height: Int = 10) {
    Box(Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(height.dp)).background(C.Soft)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height.dp).clip(RoundedCornerShape(height.dp)).background(fill))
    }
}

/** Số ngày liên tiếp đạt 5 phút (tính đến hôm nay hoặc hôm qua). */
fun streak(p: Plan, td: Long): Int {
    val passDays = p.exerciseLogs.groupBy { it.day }.filter { (_, l) -> l.sumOf { it.minutes } >= EXERCISE_PASS_MIN }.keys
    var day = if (td in passDays) td else td - 1
    var n = 0
    while (day in passDays) {
        n++; day--
    }
    return n
}

@Composable
fun WeekStrip(p: Plan, td: Long) {
    val start = LocalDate.ofEpochDay(td).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }.toEpochDay()
    val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val byDay = p.exerciseLogs.groupBy { it.day }.mapValues { (_, l) -> l.sumOf { it.minutes } }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { i, l ->
                val day = start + i
                val m = byDay[day] ?: 0
                val bg = when {
                    m >= EXERCISE_GREAT_MIN -> C.Yellow
                    m >= EXERCISE_PASS_MIN -> ExerciseDot
                    else -> C.Soft
                }
                Box(
                    Modifier.weight(1f).height(26.dp).clip(RoundedCornerShape(8.dp)).background(bg)
                        .then(if (day == td) Modifier.border(2.dp, ExerciseInk, RoundedCornerShape(8.dp)) else Modifier),
                    contentAlignment = Alignment.Center,
                ) { Text(if (m >= EXERCISE_GREAT_MIN) "★" else l, style = t(10, 800, if (m >= EXERCISE_PASS_MIN) C.Ink else C.Muted)) }
            }
        }
        Text("Xanh = đạt $EXERCISE_PASS_MIN phút · ★ = tập từ $EXERCISE_GREAT_MIN phút, được khen", style = t(11, 600, C.Muted))
    }
}

@Composable
fun StepDot(done: Boolean, color: Color) {
    Box(
        Modifier.size(26.dp).clip(CircleShape).background(if (done) color else C.Soft),
        contentAlignment = Alignment.Center,
    ) { if (done) Text("✓", style = t(14, 800, C.White)) }
}
