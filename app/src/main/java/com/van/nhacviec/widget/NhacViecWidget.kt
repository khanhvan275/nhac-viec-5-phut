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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.van.nhacviec.MainActivity
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.today
import com.van.nhacviec.ui.colors

private val KeyStart = ActionParameters.Key<String>(MainActivity.EXTRA_START_TASK)
private val KeyOpen = ActionParameters.Key<String>(MainActivity.EXTRA_OPEN_QUADRANT)

class NhacViecWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        provideContent {
            val d by Store.data.collectAsState()
            WidgetBody(d)
        }
    }
}

class NhacViecWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NhacViecWidget()
}

private fun cp(c: Color) = ColorProvider(c)
private val Ink = Color(0xFF2B3A55)
private val Muted = Color(0xFF4A5A75)

@Composable
private fun WidgetBody(d: AppData) {
    val td = today()
    Column(
        modifier = GlanceModifier.fillMaxSize().background(Color.White).cornerRadius(28.dp).padding(10.dp)
    ) {
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Tile(d, Quadrant.Q1, td, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            Tile(d, Quadrant.Q2, td, GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Tile(d, Quadrant.Q3, td, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            Tile(d, Quadrant.Q4, td, GlanceModifier.defaultWeight())
        }
        val counts = d.countdowns.filter { it.onWidget && it.daysLeft(td) >= 0 }
            .sortedBy { it.endDay }.take(2)
        if (counts.isNotEmpty()) {
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth().background(Color(0xFFEEF5FC)).cornerRadius(20.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(actionStartActivity<MainActivity>(actionParametersOf(KeyOpen to "countdown"))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                counts.forEachIndexed { i, c ->
                    if (i > 0) Spacer(GlanceModifier.width(12.dp))
                    val left = c.daysLeft(td)
                    Column(GlanceModifier.defaultWeight()) {
                        Text(c.name, maxLines = 1, style = TextStyle(color = cp(Muted), fontSize = 11.sp))
                        Text(
                            if (left == 0L) "hết hạn hôm nay" else "còn $left ngày",
                            style = TextStyle(
                                color = cp(if (left <= 5) Color(0xFFA33A45) else Color(0xFF2F5A92)),
                                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(d: AppData, q: Quadrant, td: Long, modifier: GlanceModifier) {
    val col = q.colors()
    val open = d.tasks.filter { it.quadrant == q && !it.isDone(td) }
        .sortedWith(compareBy<com.van.nhacviec.data.Task>({ it.dueAt ?: Long.MAX_VALUE }, { it.createdAt }))
    Column(
        modifier = modifier.fillMaxSize().background(col.bg).cornerRadius(22.dp).padding(10.dp)
            .clickable(actionStartActivity<MainActivity>(actionParametersOf(KeyOpen to q.name))),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "0${q.no} · ${q.label.uppercase()}",
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
                style = TextStyle(color = cp(col.ink), fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                "${open.size}",
                style = TextStyle(color = cp(col.ink), fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        if (open.isEmpty()) {
            Text("Trống — tuyệt!", style = TextStyle(color = cp(Muted), fontSize = 12.sp))
        } else {
            open.take(2).forEachIndexed { i, t ->
                Text(
                    t.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = cp(Ink),
                        fontSize = if (i == 0) 13.sp else 12.sp,
                        fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
            if (open.size > 2) {
                Text("+${open.size - 2} việc nữa", style = TextStyle(color = cp(Muted), fontSize = 11.sp))
            }
        }
        if (open.isNotEmpty() && (q == Quadrant.Q1 || q == Quadrant.Q2)) {
            Spacer(GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier.fillMaxWidth().background(Color.White).cornerRadius(999.dp)
                    .padding(vertical = 6.dp)
                    .clickable(actionStartActivity<MainActivity>(actionParametersOf(KeyStart to open.first().id))),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶ 5 phút", style = TextStyle(color = cp(col.ink), fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
