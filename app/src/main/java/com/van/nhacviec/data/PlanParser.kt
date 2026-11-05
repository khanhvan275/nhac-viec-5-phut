package com.van.nhacviec.data

/**
 * Đọc lộ trình dạng chữ (từ ChatGPT, Gemini, file .txt/.md).
 * Dạng chuẩn:
 *   LỘ TRÌNH: Fashion English
 *   LOẠI: sách
 *   - Chương 1: Fashion basics | 45 phút | ghi chú
 * Cũng đọc được danh sách đánh số, gạch đầu dòng hoặc tiêu đề markdown thông thường.
 */
object PlanParser {
    data class Parsed(val name: String, val kind: PlanKind, val steps: List<PlanStep>)

    const val PROMPT = """Hãy viết lại lộ trình theo ĐÚNG định dạng dưới đây để mình dán vào app nhắc việc. Không thêm lời giải thích, không dùng bảng.

LỘ TRÌNH: <tên lộ trình>
LOẠI: <sách | tập luyện | khóa học>
- <tên chương/bài/buổi> | <số phút mỗi lần, VD: 30 phút> | <ghi chú ngắn, có thể bỏ trống>

Mỗi dòng bắt đầu bằng dấu - là một chương/bài. Với tập luyện, mỗi dòng là một buổi trong vòng lặp (VD: Tay, Chân, Bụng, Toàn thân) và app sẽ xoay vòng mỗi ngày."""

    private val nameKeys = listOf("lộ trình", "lo trinh", "tên", "ten", "plan", "title", "khóa học", "sách")
    private val kindKeys = listOf("loại", "loai", "kind", "type")
    private val stepWords = Regex(
        "^(chương|chuong|chapter|bài|bai|lesson|unit|module|ngày|ngay|day|tuần|tuan|week|buổi|buoi|session|phần|phan|part)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val bullet = Regex("^\\s*([-*•+]|\\d+[.)]|\\[[ xX]?])\\s+")
    private val minutesRe = Regex("(\\d{1,3})\\s*(phút|phut|ph|min|mins|minutes|')", RegexOption.IGNORE_CASE)

    fun guessKind(text: String): PlanKind {
        val t = text.lowercase()
        return when {
            listOf("tập luyện", "tap luyen", "thể dục", "the duc", "giảm mỡ", "workout", "exercise", "gym", "cardio")
                .any { it in t } -> PlanKind.EXERCISE
            listOf("khóa", "khoa hoc", "course", "lesson", "bài học", "module", "bài 1").any { it in t } -> PlanKind.COURSE
            else -> PlanKind.BOOK
        }
    }

    private fun kindFrom(v: String): PlanKind? {
        val t = v.lowercase()
        return when {
            "tập" in t || "tap" in t || "exercise" in t || "workout" in t -> PlanKind.EXERCISE
            "khóa" in t || "khoa" in t || "course" in t -> PlanKind.COURSE
            "sách" in t || "sach" in t || "book" in t || "học" in t -> PlanKind.BOOK
            else -> null
        }
    }

    private fun clean(s: String): String =
        s.replace("**", "").replace("__", "").replace("`", "").replace(Regex("^#+\\s*"), "").trim()

    private fun keyValue(line: String, keys: List<String>): String? {
        val idx = line.indexOf(':')
        if (idx <= 0 || idx > 20) return null
        val k = line.substring(0, idx).trim().lowercase()
        return if (keys.any { k == it }) line.substring(idx + 1).trim() else null
    }

    fun parse(raw: String): Parsed {
        var name = ""
        var kind: PlanKind? = null
        val steps = mutableListOf<PlanStep>()
        var firstPlain: String? = null

        for (rawLine in raw.lines()) {
            if (rawLine.isBlank()) continue
            val indented = rawLine.startsWith("  ") || rawLine.startsWith("\t")
            val line = clean(rawLine)
            if (line.isBlank() || line.all { it == '-' || it == '=' || it == '|' || it == ' ' }) continue

            val nameVal = keyValue(line, nameKeys)
            if (nameVal != null) {
                if (name.isBlank()) name = clean(nameVal)
                continue
            }
            val kindVal = keyValue(line, kindKeys)
            if (kindVal != null) {
                val k = kindFrom(kindVal)
                if (k != null) kind = k
                continue
            }

            val isBullet = bullet.containsMatchIn(rawLine)
            val body = clean(rawLine.replace(bullet, ""))
            val isStepWord = stepWords.containsMatchIn(body)

            // Dòng con thụt vào dưới một bài → ghi chú của bài đó.
            if (indented && isBullet && steps.isNotEmpty() && !isStepWord) {
                val last = steps.removeAt(steps.lastIndex)
                val d = if (last.detail.isBlank()) body else last.detail + "; " + body
                steps.add(last.copy(detail = d.take(300)))
                continue
            }

            if (isBullet || isStepWord) {
                steps.add(stepFrom(body))
            } else if (rawLine.trimStart().startsWith("#") && name.isBlank()) {
                name = line
            } else if (firstPlain == null) {
                firstPlain = line
            }
        }
        if (name.isBlank()) name = (firstPlain ?: "Lộ trình mới").take(60)
        return Parsed(name.take(80), kind ?: guessKind(raw), steps)
    }

    private fun stepFrom(body: String): PlanStep {
        val parts = body.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        var title = parts.firstOrNull() ?: body
        var minutes = 0
        val details = mutableListOf<String>()
        parts.drop(1).forEach { p ->
            val m = minutesRe.find(p)
            if (minutes == 0 && m != null && p.length <= 20) minutes = m.groupValues[1].toInt()
            else details.add(p)
        }
        if (minutes == 0) {
            minutesRe.find(title)?.let { m ->
                minutes = m.groupValues[1].toInt()
                title = title.replace(Regex("\\(?\\s*" + Regex.escape(m.value) + "\\s*\\)?"), "").trim().trimEnd('-', '–', ',', ':').trim()
            }
        }
        // "Chương 1: Tên — ghi chú" → tách ghi chú sau dấu gạch dài.
        val dash = title.indexOf(" — ").takeIf { it > 0 } ?: title.indexOf(" – ").takeIf { it > 0 }
        if (dash != null) {
            details.add(0, title.substring(dash + 3).trim())
            title = title.substring(0, dash).trim()
        }
        return PlanStep(title = title.take(120), minutes = minutes, detail = details.joinToString("; ").take(300))
    }

    /** Gộp lộ trình mới vào lộ trình cũ: bài trùng tên giữ tiến độ và lịch ôn. */
    fun merge(old: Plan, p: Parsed): Plan {
        val byTitle = old.steps.associateBy { normTitle(it.title) }
        val steps = p.steps.map { s ->
            val o = byTitle[normTitle(s.title)]
            if (o == null) s else o.copy(title = s.title, detail = s.detail, minutes = s.minutes)
        }
        return old.copy(name = p.name, kind = p.kind, steps = steps, rotationIndex = old.rotationIndex.coerceAtMost((steps.size - 1).coerceAtLeast(0)))
    }
}
