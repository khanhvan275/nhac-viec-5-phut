package com.van.nhacviec.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.van.nhacviec.data.AppData
import com.van.nhacviec.data.Plan
import com.van.nhacviec.data.PlanKind
import com.van.nhacviec.data.PlanParser
import com.van.nhacviec.data.Store
import com.van.nhacviec.data.normTitle

@Composable
fun ImportPlanScreen(d: AppData, initialText: String, targetPlanId: String?, back: () -> Unit, saved: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf(initialText) }
    var kindOverride by remember { mutableStateOf<PlanKind?>(null) }
    var nameOverride by remember { mutableStateOf<String?>(null) }
    val parsed = remember(text) { PlanParser.parse(text) }
    val target = targetPlanId?.let { d.plan(it) }
    val name = nameOverride ?: if (text.isBlank() && target != null) target.name else parsed.name
    val kind = kindOverride ?: if (text.isBlank() && target != null) target.kind else parsed.kind
    val sameName = target ?: d.plans.firstOrNull { normTitle(it.name) == normTitle(name) }
    var updateOld by remember { mutableStateOf(true) }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()?.let { text = it; nameOverride = null; kindOverride = null }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = C.White, unfocusedContainerColor = C.White,
        focusedBorderColor = C.Primary, unfocusedBorderColor = Color.Transparent,
        focusedTextColor = C.Ink, unfocusedTextColor = C.Ink,
        focusedLabelColor = C.Primary, unfocusedLabelColor = C.Muted,
    )

    fun save() {
        if (parsed.steps.isEmpty()) {
            Toast.makeText(ctx, "Chưa đọc được bài nào. Thử dùng câu lệnh mẫu cho AI nhé.", Toast.LENGTH_LONG).show()
            return
        }
        val p = parsed.copy(name = name.ifBlank { parsed.name }, kind = kind)
        val plan = if (sameName != null && updateOld) PlanParser.merge(sameName, p)
        else Plan(name = p.name, kind = p.kind, steps = p.steps)
        Store.savePlan(plan)
        Toast.makeText(ctx, "Đã lưu lộ trình ${plan.name}", Toast.LENGTH_SHORT).show()
        saved()
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoundIcon("←", onClick = back)
            Text(if (target != null) "Cập nhật lộ trình" else "Nhập lộ trình", style = t(20, 800))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFCFE3F7)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("3 cách đưa lộ trình vào", style = t(13, 800, Color(0xFF1F4373)))
                Text("Chia sẻ từ app AI → chọn Nhắc Việc 5 Phút · Dán vào ô dưới · Mở file .txt / .md", style = t(12, 600))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Sao chép câu lệnh cho AI", bg = C.White, fg = Color(0xFF2F5A92), size = 12) {
                        clipboard.setText(AnnotatedString(PlanParser.PROMPT))
                        Toast.makeText(ctx, "Đã sao chép. Dán vào ChatGPT/Gemini kèm lộ trình của bạn.", Toast.LENGTH_LONG).show()
                    }
                    Pill("Mở file", bg = C.White, fg = Color(0xFF2F5A92), size = 12) {
                        openFile.launch(arrayOf("text/*"))
                    }
                }
            }

            OutlinedTextField(
                text, { text = it; nameOverride = null },
                label = { Text("Dán lộ trình vào đây") },
                shape = RoundedCornerShape(18.dp), colors = fieldColors, textStyle = t(13, 600),
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 260.dp),
            )

            if (text.isNotBlank()) {
                OutlinedTextField(
                    name, { nameOverride = it }, label = { Text("Tên lộ trình") }, singleLine = true,
                    shape = RoundedCornerShape(18.dp), colors = fieldColors, textStyle = t(15, 800),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlanKind.entries.forEach { k ->
                        Pill(
                            k.label, bg = if (k == kind) C.Primary else C.Soft, fg = if (k == kind) C.White else C.Ink,
                            size = 12, modifier = Modifier.weight(1f),
                        ) { kindOverride = k }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.White).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (parsed.steps.isEmpty()) {
                        Text("Chưa đọc được bài nào", style = t(13, 800, Color(0xFFA33A45)))
                        Text("Mỗi bài nên là một dòng bắt đầu bằng dấu - hoặc số thứ tự. Bấm \"Sao chép câu lệnh cho AI\" để AI viết đúng dạng.", style = t(12, 600, C.Muted))
                    } else {
                        Text("App đọc được ${parsed.steps.size} ${kind.stepWord}", style = t(13, 800))
                        parsed.steps.take(6).forEachIndexed { i, s ->
                            Text(
                                "${i + 1}. ${s.title}" + (if (s.minutes > 0) " · ${s.minutes}'" else ""),
                                style = t(12, 600, Color(0xFF3A4A66)), maxLines = 1,
                            )
                        }
                        if (parsed.steps.size > 6) Text("+${parsed.steps.size - 6} ${kind.stepWord} nữa", style = t(12, 600, C.Muted))
                        if (kind == PlanKind.EXERCISE) Text("Tập luyện: các buổi xoay vòng mỗi ngày, đủ 5 phút là đạt.", style = t(12, 600, C.Muted))
                    }
                }
                if (sameName != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Yellow).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Cập nhật lộ trình cũ \"${sameName.name}\"", style = t(13, 800))
                            Text("Bài trùng tên giữ nguyên tiến độ và lịch ôn", style = t(11, 600, Color(0xFF4A3D00)))
                        }
                        Switch(updateOld, { updateOld = it }, colors = SwitchDefaults.colors(checkedTrackColor = C.Primary))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Pill(
                "Lưu lộ trình",
                bg = if (parsed.steps.isEmpty()) C.Header else C.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) { save() }
        }
    }
}
