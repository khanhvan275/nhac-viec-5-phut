package com.van.nhacviec.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.van.nhacviec.Screen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun t(size: Int, weight: Int = 600, color: Color = C.Ink) =
    TextStyle(fontFamily = Nunito, fontSize = size.sp, fontWeight = FontWeight(weight), color = color)

@Composable
fun Pill(
    text: String,
    bg: Color = C.Primary,
    fg: Color = C.White,
    modifier: Modifier = Modifier,
    size: Int = 15,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = t(size, 800, fg), textAlign = TextAlign.Center)
    }
}

@Composable
fun RoundIcon(text: String, bg: Color = C.White, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = t(18, 800)) }
}

@Composable
fun BottomBar(current: Screen, tab: (Screen) -> Unit, onAdd: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(C.White).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NavItem("Hôm nay", current == Screen.Home) { tab(Screen.Home) }
            NavItem("Đếm ngày", current == Screen.Countdowns) { tab(Screen.Countdowns) }
            NavItem("Cài đặt", current == Screen.SettingsTab) { tab(Screen.SettingsTab) }
        }
        if (onAdd != null) {
            Box(
                Modifier.size(54.dp).clip(CircleShape).background(C.Primary).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Text("+", style = t(30, 700, C.White)) }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(999.dp))
            .background(if (active) C.Header else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = t(13, if (active) 800 else 700, if (active) C.Ink else C.Muted)) }
}

@Composable
fun Tag(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, style = t(11, 800, fg))
    }
}

private val dayFmt = DateTimeFormatter.ofPattern("dd/MM")
private val dayFullFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

fun fmtDue(ms: Long): String {
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val d = dt.toLocalDate()
    val now = LocalDate.now()
    val day = when (d) {
        now -> "hôm nay"
        now.plusDays(1) -> "mai"
        else -> d.format(dayFmt)
    }
    return "hạn $day ${dt.format(timeFmt)}"
}

fun fmtDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFullFmt)

fun fmtMinutes(m: Int): String = if (m < 60) "$m'" else "${m / 60}g${(m % 60).toString().padStart(2, '0')}'"

fun fmtClock(minuteOfDay: Int): String =
    "${(minuteOfDay / 60).toString().padStart(2, '0')}:${(minuteOfDay % 60).toString().padStart(2, '0')}"

private val weekdays = listOf("Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật")

fun todayLabel(): String {
    val d = LocalDate.now()
    return "${weekdays[d.dayOfWeek.value - 1]}, ${d.dayOfMonth}/${d.monthValue}"
}
