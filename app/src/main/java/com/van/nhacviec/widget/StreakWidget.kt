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
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.van.nhacviec.MainActivity
import com.van.nhacviec.R
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.Countdown
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.today
import java.time.LocalDate

/** Widget 2: streak + đếm ngày. */
class StreakWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        provideContent {
            val d by Store.data.collectAsState()
            StreakBody(d)
        }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

private val Yellow = Color(0xFFFCEFC0)
private val Orange = Color(0xFFF5A35C)
private val OrangeInk = Color(0xFFB4530E)
private val YellowInk = Color(0xFF7D5E00)
private val Soft = Color(0xFFEEF5FC)
private val Pink = Color(0xFFFBDADB)
private val PinkInk = Color(0xFFA33A45)
private val BlueInk = Color(0xFF2F5A92)

@Composable
private fun StreakBody(d: AppData) {
    val td = today()
    val streak = d.streak(td)
    val passed = d.passedDays()
    val todayDone = td in passed
    val counts = d.countdowns.filter { it.onWidget && it.daysLeft(td) >= 0 }.sortedBy { it.endDay }

    Row(
        modifier = GlanceModifier.fillMaxSize().background(Color.White).cornerRadius(26.dp).padding(8.dp),
    ) {
        // Ô streak
        Column(
            modifier = GlanceModifier.width(108.dp).fillMaxHeight().background(Yellow).cornerRadius(18.dp)
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(if (todayDone) R.drawable.ic_flame else R.drawable.ic_flame_off),
                contentDescription = null,
                modifier = GlanceModifier.size(26.dp),
            )
            Text(
                "$streak",
                style = TextStyle(color = cp(OrangeInk), fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            )
            Text(
                if (todayDone) "ngày liên tiếp" else "giữ chuỗi: 5 phút!",
                maxLines = 1,
                style = TextStyle(color = cp(YellowInk), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            )
            Spacer(GlanceModifier.height(5.dp))
            WeekDots(passed, td)
        }
        Spacer(GlanceModifier.width(8.dp))
        // Các ô đếm ngày
        if (counts.isEmpty()) {
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight().background(Soft).cornerRadius(18.dp).padding(10.dp)
                    .clickable(actionStartActivity<MainActivity>(actionParametersOf(KeyOpen to "countdown"))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Chạm để thêm phần mềm hoặc cuộc thi cần đếm ngày",
                    style = TextStyle(color = cp(WMuted), fontSize = 12.sp, textAlign = TextAlign.Center),
                )
            }
        } else {
            LazyVerticalGrid(
                gridCells = GridCells.Fixed(2),
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            ) {
                items(counts, itemId = { it.id.hashCode().toLong() }) { c -> CountTile(c, td) }
            }
        }
    }
}

@Composable
private fun WeekDots(passed: Set<Long>, td: Long) {
    val monday = LocalDate.ofEpochDay(td).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }.toEpochDay()
    Row {
        for (i in 0 until 7) {
            val day = monday + i
            if (i > 0) Spacer(GlanceModifier.width(2.dp))
            val color = when {
                day in passed -> Orange
                day == td -> Color(0xFFFFE2C2)
                day > td -> Color.White
                else -> Color(0xFFF3E4B5)
            }
            Box(GlanceModifier.size(10.dp).background(color).cornerRadius(5.dp)) {}
        }
    }
}

@Composable
private fun CountTile(c: Countdown, td: Long) {
    val left = c.daysLeft(td)
    val urgent = left <= 7
    Box(modifier = GlanceModifier.fillMaxWidth().padding(3.dp)) {
        Column(
            modifier = GlanceModifier.fillMaxWidth().background(if (urgent) Pink else Soft).cornerRadius(14.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable(actionStartActivity<MainActivity>(actionParametersOf(KeyOpen to "countdown"))),
        ) {
            Text(
                if (left == 0L) "hôm nay" else "$left ngày",
                maxLines = 1,
                style = TextStyle(color = cp(if (urgent) PinkInk else BlueInk), fontSize = 17.sp, fontWeight = FontWeight.Bold),
            )
            Text(c.name, maxLines = 1, style = TextStyle(color = cp(WInk), fontSize = 11.sp))
        }
    }
}
