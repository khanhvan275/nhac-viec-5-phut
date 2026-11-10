package com.van.nhacviec.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.van.nhacviec.MainActivity
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.Task
import com.van.nhacviec.data.today
import com.van.nhacviec.ui.colors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal val KeyStart = ActionParameters.Key<String>(MainActivity.EXTRA_START_TASK)
internal val KeyOpen = ActionParameters.Key<String>(MainActivity.EXTRA_OPEN_QUADRANT)

internal fun cp(c: Color) = ColorProvider(c)
internal val WInk = Color(0xFF2B3A55)
internal val WMuted = Color(0xFF4A5A75)

/** Cập nhật cả 2 widget. */
suspend fun updateAllWidgets(ctx: Context) {
    runCatching { NhacViecWidget().updateAll(ctx) }
    runCatching { StreakWidget().updateAll(ctx) }
}

/** Widget 1: 4 mục việc. */
class NhacViecWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        provideContent {
            val d by Store.data.collectAsState()
            TasksBody(d)
        }
    }
}

class NhacViecWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NhacViecWidget()
}

@Composable
private fun TasksBody(d: AppData) {
    val td = today()
    Column(
        modifier = GlanceModifier.fillMaxSize().background(Color.White).cornerRadius(26.dp).padding(8.dp)
    ) {
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Tile(d, Quadrant.Q1, td, GlanceModifier.defaultWeight().fillMaxHeight())
            Spacer(GlanceModifier.width(8.dp))
            Tile(d, Quadrant.Q2, td, GlanceModifier.defaultWeight().fillMaxHeight())
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Tile(d, Quadrant.Q3, td, GlanceModifier.defaultWeight().fillMaxHeight())
            Spacer(GlanceModifier.width(8.dp))
            Tile(d, Quadrant.Q4, td, GlanceModifier.defaultWeight().fillMaxHeight())
        }
    }
}

private fun shortDue(ms: Long): String {
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val day = dt.toLocalDate()
    val now = LocalDate.now()
    val time = "%02d:%02d".format(dt.hour, dt.minute)
    return when {
        day.isBefore(now) -> "quá hạn"
        day == now -> "hạn $time"
        day == now.plusDays(1) -> "hạn mai"
        else -> "hạn ${day.dayOfMonth}/${day.monthValue}"
    }
}

/** Dòng phụ dưới tên việc: hạn chót, % còn lại của lộ trình, hoặc số việc khác. */
private fun subLine(d: AppData, t: Task, others: Int, td: Long): String {
    val parts = mutableListOf<String>()
    val plan = d.planOf(t)
    val step = plan?.currentStep(td)
    if (step != null && step.progress in 1..99) parts.add("còn ${100 - step.progress}%")
    t.dueAt?.let { parts.add(shortDue(it)) }
    if (others > 0) parts.add("+$others việc")
    return parts.joinToString(" · ")
}

@Composable
private fun Tile(d: AppData, q: Quadrant, td: Long, modifier: GlanceModifier) {
    val col = q.colors()
    val open = d.tasks.filter { it.quadrant == q && !it.isDone(td) }
        .filter { t -> d.planOf(t)?.let { it.currentStep(td) != null } ?: true }
        .sortedWith(compareBy<Task>({ it.dueAt ?: Long.MAX_VALUE }, { it.createdAt }))
    val first = open.firstOrNull()

    Column(
        modifier = modifier.background(col.bg).cornerRadius(18.dp).padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>(actionParametersOf(KeyOpen to q.name))),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "0${q.no} ${q.label.uppercase()}",
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
                style = TextStyle(color = cp(col.ink), fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
            if (open.isNotEmpty()) {
                Text("${open.size}", style = TextStyle(color = cp(col.ink), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(GlanceModifier.height(3.dp))
        if (first == null) {
            Text("Trống, tuyệt!", maxLines = 1, style = TextStyle(color = cp(WMuted), fontSize = 12.sp))
        } else {
            Text(
                d.titleOf(first),
                maxLines = 2,
                style = TextStyle(color = cp(WInk), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        if (first != null) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subLine(d, first, open.size - 1, td),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                    style = TextStyle(color = cp(col.ink), fontSize = 11.sp),
                )
                if (q == Quadrant.Q1 || q == Quadrant.Q2) {
                    Box(
                        modifier = GlanceModifier.size(28.dp).background(Color.White).cornerRadius(14.dp)
                            .clickable(actionStartActivity<MainActivity>(actionParametersOf(KeyStart to first.id))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▶", style = TextStyle(color = cp(col.ink), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
