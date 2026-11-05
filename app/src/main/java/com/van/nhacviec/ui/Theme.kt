package com.van.nhacviec.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.van.nhacviec.R
import com.van.nhacviec.data.Quadrant

object C {
    val Primary = Color(0xFF3D6FB0)
    val Header = Color(0xFFB9D7F4)
    val Bg = Color(0xFFF6F9FD)
    val Soft = Color(0xFFEEF5FC)
    val Ink = Color(0xFF2B3A55)
    val Muted = Color(0xFF4A5A75)
    val White = Color(0xFFFFFFFF)
    val Yellow = Color(0xFFFCEFC0)
    val Pink = Color(0xFFFBDADB)
    val Green = Color(0xFFCDEBD8)
}

data class QColors(val bg: Color, val ink: Color, val mochi: Color)

fun Quadrant.colors(): QColors = when (this) {
    Quadrant.Q1 -> QColors(Color(0xFFFBDADB), Color(0xFFA33A45), Color(0xFFF4A9B0))
    Quadrant.Q2 -> QColors(Color(0xFFCFE3F7), Color(0xFF2F5A92), Color(0xFF9EC4EC))
    Quadrant.Q3 -> QColors(Color(0xFFFCEFC0), Color(0xFF7D5E00), Color(0xFFF5D77A))
    Quadrant.Q4 -> QColors(Color(0xFFE7DFF6), Color(0xFF5B4787), Color(0xFFCBBCEB))
}

@OptIn(ExperimentalTextApi::class)
private fun nunito(w: Int) = Font(
    R.font.nunito,
    weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val Nunito = FontFamily(nunito(400), nunito(600), nunito(700), nunito(800))

@Composable
fun NhacViecTheme(content: @Composable () -> Unit) {
    val base = Typography()
    fun TextStyle.n() = copy(fontFamily = Nunito)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Primary,
            onPrimary = C.White,
            background = C.Bg,
            surface = C.White,
            onSurface = C.Ink,
            onBackground = C.Ink,
            secondaryContainer = C.Header,
            onSecondaryContainer = C.Ink,
            surfaceVariant = C.Soft,
            onSurfaceVariant = C.Muted,
        ),
        typography = Typography(
            displaySmall = base.displaySmall.n(),
            headlineMedium = base.headlineMedium.n(),
            headlineSmall = base.headlineSmall.n(),
            titleLarge = base.titleLarge.n(),
            titleMedium = base.titleMedium.n(),
            titleSmall = base.titleSmall.n(),
            bodyLarge = base.bodyLarge.n(),
            bodyMedium = base.bodyMedium.n(),
            bodySmall = base.bodySmall.n(),
            labelLarge = base.labelLarge.n(),
            labelMedium = base.labelMedium.n(),
            labelSmall = base.labelSmall.n(),
        ),
        content = content,
    )
}
