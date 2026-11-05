package com.van.nhacviec.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

enum class Mood { HAPPY, CALM, PLEAD, SULK, ANGRY, TIRED }

/** Nhân vật mochi, vẽ trên khung 60x56. */
object Mochi {
    private const val INK = 0xFF2B3A55.toInt()

    private fun bodyPath(): Path = Path().apply {
        moveTo(8f, 28f)
        cubicTo(6f, 12f, 18f, 5f, 30f, 6f)
        cubicTo(42f, 7f, 53f, 12f, 52f, 28f)
        cubicTo(51f, 44f, 43f, 52f, 30f, 52f)
        cubicTo(17f, 52f, 10f, 44f, 8f, 28f)
        close()
    }

    fun draw(c: Canvas, width: Float, mood: Mood, body: Int) {
        val s = width / 60f
        c.save()
        c.scale(s, s)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = body; style = Paint.Style.FILL }
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK; style = Paint.Style.STROKE; strokeWidth = 2.6f
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; style = Paint.Style.FILL }

        if (mood == Mood.ANGRY) {
            val spikes = Path().apply {
                moveTo(16f, 10f); lineTo(20f, 1f); lineTo(25f, 8f); lineTo(30f, 0f)
                lineTo(35f, 8f); lineTo(40f, 1f); lineTo(44f, 10f); close()
            }
            c.drawPath(spikes, fill)
        }
        c.drawPath(bodyPath(), fill)

        when (mood) {
            Mood.HAPPY -> {
                c.drawPath(Path().apply { moveTo(19f, 24f); lineTo(24f, 27f); lineTo(19f, 30f) }, ink)
                c.drawPath(Path().apply { moveTo(41f, 24f); lineTo(36f, 27f); lineTo(41f, 30f) }, ink)
                c.drawPath(Path().apply { moveTo(26f, 34f); quadTo(30f, 38f, 34f, 34f) }, ink)
            }
            Mood.CALM -> {
                c.drawPath(Path().apply { moveTo(19f, 27f); quadTo(22f, 24f, 25f, 27f) }, ink)
                c.drawPath(Path().apply { moveTo(35f, 27f); quadTo(38f, 24f, 41f, 27f) }, ink)
                c.drawPath(Path().apply { moveTo(26f, 34f); quadTo(30f, 37f, 34f, 34f) }, ink)
            }
            Mood.PLEAD -> {
                c.drawCircle(22f, 26f, 3.2f, dot)
                c.drawCircle(38f, 26f, 3.2f, dot)
                val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
                c.drawCircle(23f, 25f, 1f, white)
                c.drawCircle(39f, 25f, 1f, white)
                c.drawPath(Path().apply { moveTo(26f, 36f); quadTo(30f, 33f, 34f, 36f) }, ink)
                val tear = Paint(ink).apply { color = 0xFF8FB8E6.toInt(); strokeWidth = 2.4f }
                c.drawPath(Path().apply { moveTo(15f, 32f); quadTo(14f, 36f, 16f, 38f) }, tear)
            }
            Mood.SULK -> {
                c.drawLine(18f, 25f, 25f, 25f, ink)
                c.drawLine(35f, 25f, 42f, 25f, ink)
                c.drawLine(26f, 35f, 34f, 35f, ink)
                val blush = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4A9B0.toInt() }
                c.drawOval(RectF(13.5f, 30f, 20.5f, 34f), blush)
                c.drawOval(RectF(39.5f, 30f, 46.5f, 34f), blush)
            }
            Mood.ANGRY -> {
                c.drawLine(18f, 22f, 25f, 26f, ink)
                c.drawLine(42f, 22f, 35f, 26f, ink)
                c.drawPath(Path().apply { moveTo(25f, 37f); quadTo(30f, 33f, 35f, 37f) }, ink)
            }
            Mood.TIRED -> {
                c.drawLine(18f, 27f, 24f, 29f, ink)
                c.drawLine(42f, 27f, 36f, 29f, ink)
                c.drawLine(27f, 35f, 33f, 35f, ink)
            }
        }
        c.restore()
    }

    fun bitmap(sizePx: Int, mood: Mood, body: Int): Bitmap {
        val h = (sizePx * 56f / 60f).toInt()
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.translate(0f, (sizePx - h) / 2f)
        draw(c, sizePx.toFloat(), mood, body)
        return b
    }
}

@Composable
fun MochiView(mood: Mood, color: Color, size: Dp, modifier: Modifier = Modifier) {
    ComposeCanvas(modifier.size(size)) {
        drawIntoCanvas { Mochi.draw(it.nativeCanvas, this.size.width, mood, color.toArgb()) }
    }
}
