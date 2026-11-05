package com.van.nhacviec.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PlanKind(val label: String, val stepWord: String) {
    BOOK("Sách / học", "chương"),
    EXERCISE("Tập luyện", "buổi"),
    COURSE("Khóa học", "bài"),
}

/** Khoảng cách ôn tập (ngày) sau khi học xong một chương. */
val REVIEW_DAYS = listOf(1, 3, 7, 14, 30)

/** Tập luyện: đủ 5 phút là đạt, từ 15 phút được khen. */
const val EXERCISE_PASS_MIN = 5
const val EXERCISE_GREAT_MIN = 15

@Serializable
data class PlanStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val detail: String = "",
    val minutes: Int = 0,
    /** 0..100 */
    val progress: Int = 0,
    /** Tiến độ trước khi học hôm nay (để biết hôm nay học thêm bao nhiêu). */
    val progressBefore: Int = 0,
    val progressDay: Long = 0,
    val doneDay: Long? = null,
    /** Số lần đã ôn. */
    val reviewStage: Int = 0,
    /** Ngày cần ôn tiếp (epochDay), null = không cần ôn. */
    val nextReview: Long? = null,
) {
    val done: Boolean get() = progress >= 100
}

@Serializable
data class ExerciseLog(val day: Long, val stepId: String, val minutes: Int)

@Serializable
data class Plan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: PlanKind = PlanKind.BOOK,
    val quadrant: Quadrant = Quadrant.Q2,
    val steps: List<PlanStep> = emptyList(),
    /** Việc hằng ngày gắn với lộ trình (để nhắc, bấm giờ, hiện trên widget). */
    val taskId: String = UUID.randomUUID().toString(),
    val rotationIndex: Int = 0,
    val lastPassDay: Long = 0,
    val lastPassIndex: Int = 0,
    val exerciseLogs: List<ExerciseLog> = emptyList(),
    val reviewNotifiedDay: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Bài/chương hôm nay. */
    fun currentStep(today: Long = today()): PlanStep? {
        if (steps.isEmpty()) return null
        return when (kind) {
            PlanKind.EXERCISE -> {
                val idx = if (lastPassDay == today) lastPassIndex else rotationIndex
                steps[idx.mod(steps.size)]
            }
            else -> steps.firstOrNull { !it.done }
        }
    }

    fun nextExerciseStep(today: Long = today()): PlanStep? {
        if (kind != PlanKind.EXERCISE || steps.isEmpty()) return null
        val idx = if (lastPassDay == today) lastPassIndex + 1 else rotationIndex + 1
        return steps[idx.mod(steps.size)]
    }

    fun doneCount(): Int = steps.count { it.done }

    fun overallPercent(): Int =
        if (steps.isEmpty()) 0 else steps.sumOf { it.progress.coerceIn(0, 100) } / steps.size

    fun dueReviews(today: Long = today()): List<PlanStep> =
        if (kind == PlanKind.EXERCISE) emptyList()
        else steps.filter { it.nextReview != null && it.nextReview <= today }

    fun exerciseMinutesToday(today: Long = today()): Int =
        exerciseLogs.filter { it.day == today }.sumOf { it.minutes }

    /** Câu mô tả việc hôm nay, dùng trong thẻ việc và lời nhắc. */
    fun todayText(today: Long = today()): String {
        val s = currentStep(today) ?: return if (steps.isEmpty()) "Chưa có bài nào" else "Đã học xong cả lộ trình!"
        return when (kind) {
            PlanKind.EXERCISE -> {
                val m = exerciseMinutesToday(today)
                if (lastPassDay == today) "Hôm nay đã tập ${s.title} $m phút. Mai: ${nextExerciseStep(today)?.title ?: ""}"
                else "Hôm nay: ${s.title} · tập ≥ $EXERCISE_PASS_MIN phút là đạt"
            }
            else -> when {
                s.progress <= 0 -> "Hôm nay: ${s.title}"
                s.progressDay == today -> "${s.title}: hôm nay đã học đến ${s.progress}%"
                else -> "${s.title}: học nốt ${100 - s.progress}% còn lại (đã ${s.progress}%)"
            }
        }
    }

    fun shortTitle(today: Long = today()): String {
        val s = currentStep(today) ?: return name
        return "$name · ${s.title}"
    }
}

/** Ghi tiến độ chờ Vân xác nhận sau khi bấm giờ xong. */
@Serializable
data class PendingLog(
    val planId: String,
    val stepId: String,
    val minutes: Int,
    val review: Boolean = false,
    val exerciseTotal: Int = 0,
    val passed: Boolean = false,
)

fun normTitle(s: String): String =
    s.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
