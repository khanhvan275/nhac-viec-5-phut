package com.van.nhacviec.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.Task
import com.van.nhacviec.data.today
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun EditTaskScreen(existing: Task?, defaultQ: Quadrant, back: () -> Unit) {
    val ctx = LocalContext.current
    val td = today()
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var detail by remember { mutableStateOf(existing?.detail ?: "") }
    var dayDetail by remember { mutableStateOf(existing?.todayDetail(td) ?: "") }
    var q by remember { mutableStateOf(existing?.quadrant ?: defaultQ) }
    var daily by remember { mutableStateOf(existing?.daily ?: false) }
    var dueAt by remember { mutableStateOf(existing?.dueAt) }

    fun pickDue() {
        val start = dueAt?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
            ?: LocalDateTime.now().plusHours(3).withMinute(0)
        DatePickerDialog(ctx, { _, y, m, day ->
            TimePickerDialog(ctx, { _, h, min ->
                dueAt = LocalDateTime.of(y, m + 1, day, h, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }, start.hour, start.minute, true).show()
        }, start.year, start.monthValue - 1, start.dayOfMonth).show()
    }

    fun save() {
        if (title.isBlank()) return
        val base = existing ?: Task(title = title.trim(), quadrant = q)
        Store.upsertTask(
            base.copy(
                title = title.trim(),
                detail = detail.trim(),
                quadrant = q,
                daily = daily,
                dueAt = if (daily) null else dueAt,
                dayDetail = if (daily) dayDetail.trim() else "",
                dayDetailDate = if (daily) td else 0,
                // Đổi mục thì bắt đầu nhắc lại từ đầu.
                nagLevel = if (existing != null && existing.quadrant != q) 0 else base.nagLevel,
            )
        )
        back()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = C.Soft, unfocusedContainerColor = C.Soft,
        focusedBorderColor = C.Primary, unfocusedBorderColor = Color.Transparent,
        focusedTextColor = C.Ink, unfocusedTextColor = C.Ink,
        focusedLabelColor = C.Primary, unfocusedLabelColor = C.Muted,
    )

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoundIcon("←", onClick = back)
            Text(if (existing == null) "Thêm việc" else "Sửa việc", style = t(20, 800), modifier = Modifier.weight(1f))
            if (existing != null) {
                Text("Xóa", style = t(14, 800, Color(0xFFA33A45)),
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable {
                        Store.deleteTask(existing.id); back()
                    }.padding(10.dp))
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(C.White).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Tên việc") },
                    singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors,
                    textStyle = t(15, 700), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(detail, { detail = it },
                    label = { Text(if (daily) "Mô tả chung (VD: tập theo bài của Kimi)" else "Chi tiết") },
                    shape = RoundedCornerShape(18.dp), colors = fieldColors, textStyle = t(14, 600),
                    modifier = Modifier.fillMaxWidth())
                if (daily) {
                    OutlinedTextField(dayDetail, { dayDetail = it },
                        label = { Text("Hôm nay làm gì? (VD: đọc chương 1)") },
                        shape = RoundedCornerShape(18.dp), colors = fieldColors, textStyle = t(14, 600),
                        modifier = Modifier.fillMaxWidth())
                }
            }

            Text("Mục", style = t(13, 800, C.Muted), modifier = Modifier.padding(start = 4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QChip(Quadrant.Q1, q == Quadrant.Q1, Modifier.weight(1f)) { q = Quadrant.Q1 }
                    QChip(Quadrant.Q2, q == Quadrant.Q2, Modifier.weight(1f)) { q = Quadrant.Q2 }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QChip(Quadrant.Q3, q == Quadrant.Q3, Modifier.weight(1f)) { q = Quadrant.Q3 }
                    QChip(Quadrant.Q4, q == Quadrant.Q4, Modifier.weight(1f)) { q = Quadrant.Q4 }
                }
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.White).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Việc cố định hằng ngày", style = t(15, 800))
                    Text("Tự xuất hiện lại mỗi sáng", style = t(12, 600, C.Muted))
                }
                Switch(daily, { daily = it }, colors = SwitchDefaults.colors(checkedTrackColor = C.Primary))
            }

            if (!daily) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.White)
                        .clickable { pickDue() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Hạn chót", style = t(15, 800))
                        Text(dueAt?.let { fmtDue(it) } ?: "Không có hạn · chạm để chọn", style = t(12, 600, C.Muted))
                    }
                    if (dueAt != null) {
                        Text("Bỏ hạn", style = t(13, 800, C.Primary),
                            modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable { dueAt = null }.padding(8.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Pill(
                "Lưu",
                bg = if (title.isBlank()) C.Header else C.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) { save() }
        }
    }
}

@Composable
private fun QChip(q: Quadrant, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val col = q.colors()
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(col.bg)
            .then(if (selected) Modifier.border(3.dp, col.ink, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(onClick = onClick).padding(12.dp),
    ) {
        Text("0${q.no} · ${q.label}", style = t(13, 800, col.ink))
        Text(q.title, style = t(11, 600, C.Ink))
    }
}
