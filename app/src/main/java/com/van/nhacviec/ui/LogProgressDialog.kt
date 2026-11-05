package com.van.nhacviec.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.EXERCISE_GREAT_MIN
import com.van.nhacviec.data.EXERCISE_PASS_MIN
import com.van.nhacviec.data.PendingLog
import com.van.nhacviec.data.PlanKind
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.today
import com.van.nhacviec.notify.Notifier

/** Hộp thoại sau giờ học/tập: ghi %, khen khi tập, hoặc hỏi kết quả ôn. */
@Composable
fun LogProgressDialog(d: AppData, pl: PendingLog) {
    val ctx = LocalContext.current
    val plan = d.plan(pl.planId)
    val step = plan?.steps?.firstOrNull { it.id == pl.stepId }
    LaunchedEffect(pl) { Notifier.cancel(ctx, Notifier.ID_LOG) }
    if (plan == null || step == null) {
        LaunchedEffect(Unit) { Store.setPending(null) }
        return
    }
    val close = { Store.setPending(null) }

    when {
        pl.review -> AlertDialog(
            onDismissRequest = {},
            containerColor = C.White,
            shape = RoundedCornerShape(28.dp),
            icon = { MochiView(Mood.CALM, C.Yellow, 56.dp) },
            title = { Text("Ôn xong ${step.title}!", style = t(18, 800)) },
            text = { Text("Lần này nhớ được nhiều không? Nhớ tốt thì lần ôn sau sẽ giãn ra xa hơn.", style = t(14, 600)) },
            confirmButton = { Pill("Nhớ tốt", size = 14) { Store.review(plan.id, step.id, remembered = true) } },
            dismissButton = { Pill("Quên nhiều", bg = C.Soft, fg = C.Ink, size = 14) { Store.review(plan.id, step.id, remembered = false) } },
        )

        plan.kind == PlanKind.EXERCISE && pl.exerciseTotal < 0 -> AlertDialog(
            onDismissRequest = close,
            containerColor = C.White,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Hôm nay tập ${step.title} bao lâu?", style = t(18, 800)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Đã tập: ${plan.exerciseMinutesToday()} phút", style = t(13, 600, C.Muted))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 20, 30).forEach { m ->
                            Pill("$m'", bg = C.Soft, fg = C.Ink, size = 13, modifier = Modifier.weight(1f)) {
                                Store.logExercise(plan.id, m)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { Pill("Hủy", bg = C.Soft, fg = C.Ink, size = 14, onClick = close) },
        )

        plan.kind == PlanKind.EXERCISE -> {
            val total = pl.exerciseTotal
            val (mood, title, body) = when {
                total >= EXERCISE_GREAT_MIN -> Triple(Mood.HAPPY, "$total phút ${step.title}! Amazing!",
                    "Gấp ${total / EXERCISE_PASS_MIN} lần yêu cầu. Mai tới ${plan.currentStep(today() + 1)?.title ?: ""} nhé, chỉ cần 5 phút.")
                total >= EXERCISE_PASS_MIN -> Triple(Mood.HAPPY, "Đạt rồi! $total phút hôm nay",
                    "Mai tới ${plan.currentStep(today() + 1)?.title ?: ""}. Giữ chuỗi nha!")
                else -> Triple(Mood.PLEAD, "Mới $total phút thôi",
                    "Thêm ${EXERCISE_PASS_MIN - total} phút nữa là đạt hôm nay. Bạn làm được mà!")
            }
            AlertDialog(
                onDismissRequest = close,
                containerColor = if (total >= EXERCISE_PASS_MIN) ExerciseGreen else C.White,
                shape = RoundedCornerShape(28.dp),
                icon = { MochiView(mood, C.White, 64.dp) },
                title = { Text(title, style = t(18, 800)) },
                text = { Text(body, style = t(14, 600)) },
                confirmButton = { Pill(if (total >= EXERCISE_PASS_MIN) "Tuyệt!" else "Ok", size = 14, onClick = close) },
            )
        }

        else -> {
            var pct by remember(pl) { mutableFloatStateOf(step.progress.toFloat().coerceAtLeast(0f)) }
            val before = if (step.progressDay == today()) step.progressBefore else step.progress
            AlertDialog(
                onDismissRequest = {},
                containerColor = C.White,
                shape = RoundedCornerShape(28.dp),
                icon = { MochiView(Mood.HAPPY, C.Header, 56.dp) },
                title = {
                    Text(
                        if (pl.minutes > 0) "Học được ${pl.minutes} phút, giỏi lắm!" else "Ghi tiến độ",
                        style = t(18, 800),
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${step.title} đã xong đến đâu rồi?", style = t(14, 600))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                Slider(
                                    value = pct, onValueChange = { pct = it }, valueRange = 0f..100f, steps = 19,
                                    colors = SliderDefaults.colors(thumbColor = C.Primary, activeTrackColor = Color(0xFF9EC4EC)),
                                )
                            }
                            Text("${pct.toInt()}%", style = t(18, 800, Color(0xFF2F5A92)))
                        }
                        if (before > 0) {
                            val more = pct.toInt() - before
                            Text(
                                "Trước hôm nay: $before%" + if (more > 0) " · hôm nay thêm $more%" else "",
                                style = t(12, 600, C.Muted),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(25, 50, 75, 100).forEach { v ->
                                Pill(
                                    if (v == 100) "Xong" else "$v%",
                                    bg = if (pct.toInt() == v) C.Primary else C.Soft,
                                    fg = if (pct.toInt() == v) C.White else C.Ink,
                                    size = 13, modifier = Modifier.weight(1f),
                                ) { pct = v.toFloat() }
                            }
                        }
                        if (pct >= 100f) Text("Xong chương! App sẽ nhắc ôn lại sau 1, 3, 7, 14, 30 ngày.", style = t(12, 700, Color(0xFF2F6B45)))
                    }
                },
                confirmButton = { Pill("Lưu tiến độ", size = 14) { Store.logProgress(plan.id, step.id, pct.toInt()) } },
                dismissButton = { Pill("Để sau", bg = C.Soft, fg = C.Ink, size = 14, onClick = close) },
            )
        }
    }
}
