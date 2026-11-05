package com.van.nhacviec.notify

import com.van.nhacviec.ui.Mood

/**
 * Câu nhắc theo 4 mức, tăng dần như Duolingo.
 * {t} = tên việc. Muốn thêm câu mới chỉ cần thêm vào danh sách.
 */
object Messages {
    data class Line(val title: String, val body: String)

    private val levels: List<List<Line>> = listOf(
        // Mức 0: nhẹ nhàng
        listOf(
            Line("{t} ơi, đến giờ rồi", "Just 5 minutes! Làm một chút thôi là được."),
            Line("Thử 5 phút nhé?", "{t} — bắt đầu nhỏ thôi, small steps count."),
            Line("Hi Vân!", "Có 5 phút không? {t} đang chờ đó."),
            Line("Một chút thôi", "5 phút cho {t}. You got this!"),
        ),
        // Mức 1: năn nỉ
        listOf(
            Line("Năn nỉ đó, 5 phút thôi mà", "Please? {t} đang ngồi khóc một mình."),
            Line("Pretty please?", "5 phút {t} thôi, xong mình hết làm phiền."),
            Line("Vân ơi...", "{t} buồn lắm rồi đó. Just 5 minutes?"),
            Line("Đi mà", "Chỉ 5 phút {t}. Mình hứa không lâu đâu."),
        ),
        // Mức 2: dỗi
        listOf(
            Line("Thôi, không thèm nhắc nữa...", "...đùa đấy. Fine, I'll wait. Nhưng {t} 5 phút thôi nhé."),
            Line("Hmph.", "Mình dỗi rồi. Làm {t} 5 phút thì mình hết dỗi."),
            Line("Ok... cứ để đó vậy", "{t} vẫn chờ. Still here. Still waiting."),
        ),
        // Mức 3: cà khịa
        listOf(
            Line("Lần thứ mấy rồi đấy, chị đại?", "Lướt điện thoại được mà 5 phút {t} thì không? Seriously?"),
            Line("Plot twist: {t} không tự làm", "Mình kiểm tra rồi, nó vẫn chưa xong. 5 phút thôi mà!"),
            Line("Breaking news", "{t} vẫn chưa được làm. Cả thế giới đang chờ (và mình nữa)."),
            Line("Excuse me?", "{t} cần 5 phút, không phải 5 năm. Let's go!"),
        ),
    )

    fun pick(level: Int, task: String): Line {
        val l = levels[level.coerceIn(0, levels.lastIndex)].random()
        return Line(l.title.replace("{t}", task), l.body.replace("{t}", task))
    }

    fun mood(level: Int): Mood = when (level) {
        0 -> Mood.HAPPY
        1 -> Mood.PLEAD
        2 -> Mood.SULK
        else -> Mood.ANGRY
    }
}
