package com.van.nhacviec.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.van.nhacviec.Screen
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.Countdown
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.Task
import com.van.nhacviec.data.today
import java.time.LocalDate

private val PinkInk = Color(0xFFA33A45)
private val BlueInk = Color(0xFF2F5A92)

@Composable
fun CountdownScreen(d: AppData, nav: (Screen) -> Unit, tab: (Screen) -> Unit) {
    val td = today()
    var editing by remember { mutableStateOf<Countdown?>(null) }
    var adding by remember { mutableStateOf(false) }
    val software = d.countdowns.filter { !it.contest }.sortedBy { it.endDay }
    val contests = d.countdowns.filter { it.contest }.sortedBy { it.endDay }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đếm ngày", style = t(24, 800), modifier = Modifier.weight(1f))
                Pill("+ Thêm", size = 13) { adding = true }
            }
            Text(
                "Phần mềm, cuộc thi, hạn bất kỳ. Hết chu trình app tự chạy tiếp, trừ khi bạn bấm Dừng.",
                style = t(13, 600, C.Muted),
            )
            if (d.countdowns.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.White).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MochiView(Mood.CALM, C.Header, 64.dp)
                    Text("Chưa có gì. Thêm CLO3D hay cuộc thi đầu tiên nhé!", style = t(14, 700))
                }
            }
            if (software.isNotEmpty()) Text("PHẦN MỀM", style = t(12, 800, C.Muted), modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            software.forEach { c -> CountCard(c, td) { editing = c } }
            if (contests.isNotEmpty()) Text("CUỘC THI", style = t(12, 800, C.Muted), modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            contests.forEach { c -> CountCard(c, td) { editing = c } }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Yellow).padding(14.dp),
            ) {
                Text("Thông báo", style = t(13, 800))
                Text(
                    "Trước 5 ngày, trước 1 ngày, và ngày hết chu trình (kèm nút chạy tiếp hoặc dừng). Còn lại xem trên widget.",
                    style = t(13, 600, Color(0xFF4A3D00)),
                )
            }
        }
        BottomBar(Screen.Countdowns, tab)
    }

    if (adding || editing != null) {
        CountdownDialog(editing) { adding = false; editing = null }
    }
}

@Composable
private fun CountCard(c: Countdown, td: Long, onEdit: () -> Unit) {
    val left = c.daysLeft(td)
    val ended = left < 0
    val urgent = left in 0..7
    val bg = when {
        ended -> Color(0xFFEAECEF)
        urgent -> C.Pink
        c.contest -> Color(0xFFCFE3F7)
        else -> C.White
    }
    val ink = if (urgent) PinkInk else BlueInk
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(bg).clickable(onClick = onEdit).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.name, style = t(17, 800))
                Text(
                    (if (c.contest) "Nộp " else "Hết ") + fmtDay(c.endDay) + if (c.onWidget) " · trên widget" else "",
                    style = t(12, 600, C.Muted),
                )
                Text(
                    if (c.renews()) "Chu trình ${c.cycleLabel()} · tự chạy tiếp" + (if (c.renewals > 0) " · lần ${c.renewals + 1}" else "")
                    else "Không tự chạy tiếp",
                    style = t(12, 700, if (c.renews()) BlueInk else C.Muted),
                )
            }
            Column(
                Modifier.clip(RoundedCornerShape(18.dp)).background(C.White).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (ended) "hết" else if (left == 0L) "0" else "$left", style = t(24, 800, if (ended) C.Muted else ink))
                Text(if (ended) "hạn" else "ngày", style = t(11, 700, if (ended) C.Muted else ink))
            }
        }
        if (!ended) {
            val span = (c.endDay - c.startDay).coerceAtLeast(1)
            val used = ((td - c.startDay).toFloat() / span).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xB3FFFFFF))) {
                Box(Modifier.fillMaxWidth(used).height(10.dp).clip(RoundedCornerShape(5.dp)).background(if (urgent) Color(0xFFF4A9B0) else Color(0xFF9EC4EC)))
            }
        }

        when {
            // Hết chu trình hôm nay: hỏi chạy tiếp hay dừng; không chọn thì mai tự chạy.
            left == 0L && c.renews() -> Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.White).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MochiView(Mood.PLEAD, Color(0xFFFBDADB), 36.dp)
                    Text(
                        "Hết chu trình hôm nay. Không chọn gì thì mai app tự chạy tiếp ${c.cycleLabel()}.",
                        style = t(13, 600), modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(if (c.isMonthly()) "Tiếp 1 tháng" else "Lặp lại ${c.cycleLabel()}", size = 12, modifier = Modifier.weight(1f)) {
                        Store.renewCountdown(c.id, oneMonth = false)
                    }
                    if (!c.isMonthly()) {
                        Pill("Tiếp 1 tháng", bg = C.Soft, fg = C.Ink, size = 12, modifier = Modifier.weight(1f)) {
                            Store.renewCountdown(c.id, oneMonth = true)
                        }
                    }
                    Pill("Dừng", bg = C.Soft, fg = PinkInk, size = 12) { Store.stopCountdown(c.id) }
                }
            }

            // Đã dừng và đã hết hạn: cho chạy lại.
            ended -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Chạy lại ${c.cycleLabel()}", size = 12, modifier = Modifier.weight(1f)) {
                    val start = today()
                    Store.upsertCountdown(
                        c.copy(autoRenew = true, startDay = start, endDay = c.copy(endDay = start).nextEnd(), notified = emptyList())
                    )
                }
                Pill("Xóa", bg = C.White, fg = PinkInk, size = 12) { Store.deleteCountdown(c.id) }
            }

            !c.contest && left in 1..7 -> {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.White).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MochiView(Mood.PLEAD, Color(0xFFFBDADB), 36.dp)
                    Text("Còn $left ngày ${c.name}! Tận dụng nốt nhé.", style = t(13, 600), modifier = Modifier.weight(1f))
                }
                Pill("Thêm \"Dùng ${c.name} 30 phút\" vào mục 02", bg = C.White, fg = PinkInk, size = 13, modifier = Modifier.fillMaxWidth()) {
                    Store.upsertTask(Task(title = "Dùng ${c.name} 30 phút", detail = "Tận dụng trước khi hết hạn ${fmtDay(c.endDay)}", quadrant = Quadrant.Q2))
                }
            }
        }
    }
}

@Composable
private fun CountdownDialog(existing: Countdown?, close: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var contest by remember { mutableStateOf(existing?.contest ?: false) }
    var onWidget by remember { mutableStateOf(existing?.onWidget ?: true) }
    var endDay by remember { mutableStateOf(existing?.endDay ?: (today() + 30)) }
    var unit by remember { mutableStateOf(existing?.cycleUnit ?: "month") }
    var countText by remember { mutableStateOf((existing?.cycleCount ?: 1).toString()) }
    var renew by remember { mutableStateOf(existing?.renews() ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = close,
        containerColor = C.White,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (existing == null) "Thêm đếm ngày" else "Sửa đếm ngày", style = t(19, 800)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên (VD: CLO3D)") }, singleLine = true,
                    shape = RoundedCornerShape(16.dp), textStyle = t(15, 700), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Phần mềm", bg = if (!contest) C.Primary else C.Soft, fg = if (!contest) C.White else C.Ink, size = 13,
                        modifier = Modifier.weight(1f)) {
                        contest = false
                        if (existing == null) renew = true
                    }
                    Pill("Cuộc thi", bg = if (contest) C.Primary else C.Soft, fg = if (contest) C.White else C.Ink, size = 13,
                        modifier = Modifier.weight(1f)) {
                        contest = true
                        if (existing == null) renew = false
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Soft).clickable {
                        val dt = LocalDate.ofEpochDay(endDay)
                        DatePickerDialog(ctx, { _, y, m, day -> endDay = LocalDate.of(y, m + 1, day).toEpochDay() },
                            dt.year, dt.monthValue - 1, dt.dayOfMonth).show()
                    }.padding(14.dp),
                ) {
                    Text(if (contest) "Hạn nộp: " else "Hết hạn chu trình này: ", style = t(14, 600, C.Muted))
                    Text(fmtDay(endDay), style = t(14, 800))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tự chạy chu trình mới khi hết", style = t(14, 700))
                        Text("Hết hạn mà không bấm Dừng thì app tự gia hạn", style = t(11, 600, C.Muted))
                    }
                    Switch(renew, { renew = it }, colors = SwitchDefaults.colors(checkedTrackColor = C.Primary))
                }
                if (renew) {
                    Text("Mỗi chu trình dài", style = t(13, 700, C.Muted))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            countText, { v -> countText = v.filter { it.isDigit() }.take(3) },
                            singleLine = true, shape = RoundedCornerShape(16.dp), textStyle = t(15, 800),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp),
                        )
                        listOf("day" to "ngày", "month" to "tháng", "year" to "năm").forEach { (k, label) ->
                            Pill(label, bg = if (unit == k) C.Primary else C.Soft, fg = if (unit == k) C.White else C.Ink,
                                size = 13, modifier = Modifier.weight(1f)) { unit = k }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hiện trên widget", style = t(14, 700), modifier = Modifier.weight(1f))
                    Switch(onWidget, { onWidget = it }, colors = SwitchDefaults.colors(checkedTrackColor = C.Primary))
                }
                if (existing != null) {
                    Text(
                        if (confirmDelete) "Chạm lần nữa để xóa hẳn" else "Xóa mục này",
                        style = t(14, 800, PinkInk),
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable {
                            if (confirmDelete) {
                                Store.deleteCountdown(existing.id); close()
                            } else confirmDelete = true
                        }.padding(6.dp),
                    )
                }
            }
        },
        confirmButton = {
            Pill("Lưu", size = 14) {
                if (name.isNotBlank()) {
                    val n = countText.toIntOrNull()?.coerceIn(1, 999) ?: 1
                    val base = existing ?: Countdown(name = name.trim(), endDay = endDay)
                    Store.upsertCountdown(
                        base.copy(
                            name = name.trim(), contest = contest, onWidget = onWidget, endDay = endDay,
                            cycleUnit = unit, cycleCount = n, autoRenew = renew,
                            notified = if (existing != null && existing.endDay != endDay) emptyList() else base.notified,
                        )
                    )
                    close()
                }
            }
        },
        dismissButton = { Pill("Hủy", bg = C.Soft, fg = C.Ink, size = 14, onClick = close) },
    )
}
