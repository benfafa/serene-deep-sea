package com.fanqieyu.deepfocus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BrandLogoMark(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(1.dp, Color(0x55FFFFFF), shape),
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "番茄鱼钟 Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun NavIconAquarium(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val wave = Path().apply {
            moveTo(w * 0.1f, h * 0.56f)
            cubicTo(w * 0.3f, h * 0.38f, w * 0.48f, h * 0.72f, w * 0.9f, h * 0.48f)
        }
        drawPath(wave, color = color, style = Stroke(width = w * 0.095f, cap = StrokeCap.Round))
        drawOval(
            color = color,
            topLeft = Offset(w * 0.26f, h * 0.22f),
            size = Size(w * 0.42f, h * 0.26f),
            style = Stroke(width = w * 0.085f),
        )
        val tail = Path().apply {
            moveTo(w * 0.68f, h * 0.35f)
            lineTo(w * 0.84f, h * 0.22f)
            lineTo(w * 0.84f, h * 0.48f)
            close()
        }
        drawPath(tail, color = color)
    }
}

@Composable
fun NavIconScenes(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.14f, h * 0.18f),
            size = Size(w * 0.72f, h * 0.64f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = stroke,
        )
        val mountain = Path().apply {
            moveTo(w * 0.22f, h * 0.68f)
            lineTo(w * 0.44f, h * 0.44f)
            lineTo(w * 0.58f, h * 0.58f)
            lineTo(w * 0.72f, h * 0.42f)
            lineTo(w * 0.80f, h * 0.68f)
        }
        drawPath(mountain, color = color, style = stroke)
        drawCircle(color = color, radius = w * 0.065f, center = Offset(w * 0.34f, h * 0.34f))
    }
}

@Composable
fun NavIconNursery(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val body = Path().apply {
            moveTo(w * 0.16f, h * 0.50f)
            cubicTo(w * 0.34f, h * 0.22f, w * 0.60f, h * 0.24f, w * 0.74f, h * 0.50f)
            cubicTo(w * 0.60f, h * 0.76f, w * 0.34f, h * 0.78f, w * 0.16f, h * 0.50f)
            close()
        }
        drawPath(body, color = color, style = stroke)
        val tail = Path().apply {
            moveTo(w * 0.72f, h * 0.50f)
            lineTo(w * 0.90f, h * 0.30f)
            lineTo(w * 0.90f, h * 0.70f)
            close()
        }
        drawPath(tail, color = color)
        drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.32f, h * 0.46f))
    }
}

@Composable
fun NavIconSettings(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        listOf(0.28f, 0.50f, 0.72f).forEachIndexed { idx, yFrac ->
            val y = h * yFrac
            drawLine(color, Offset(w * 0.16f, y), Offset(w * 0.84f, y), strokeWidth = w * 0.085f, cap = StrokeCap.Round)
            val knobX = w * when (idx) {
                0 -> 0.36f
                1 -> 0.66f
                else -> 0.46f
            }
            drawCircle(color = color, radius = w * 0.09f, center = Offset(knobX, y), style = stroke)
        }
    }
}

@Composable
fun CoinGlyphIcon(size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFE59E), Color(0xFFD99B26)),
            ),
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.5f),
        )
        drawCircle(
            color = Color(0x99FFF8DC),
            radius = w * 0.28f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.08f),
        )
    }
}

@Composable
fun WeatherGlyphIcon(
    weatherType: WeatherType,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        when (weatherType) {
            WeatherType.SUNNY -> {
                drawCircle(Color(0xFFF8C65B), radius = w * 0.24f, center = Offset(w * 0.5f, h * 0.5f))
                for (i in 0 until 8) {
                    val angle = (i * 45.0) * Math.PI / 180.0
                    val sx = (w * 0.5f + kotlin.math.cos(angle) * w * 0.32f).toFloat()
                    val sy = (h * 0.5f + kotlin.math.sin(angle) * h * 0.32f).toFloat()
                    val ex = (w * 0.5f + kotlin.math.cos(angle) * w * 0.44f).toFloat()
                    val ey = (h * 0.5f + kotlin.math.sin(angle) * h * 0.44f).toFloat()
                    drawLine(Color(0xFFF8C65B), Offset(sx, sy), Offset(ex, ey), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                }
            }
            WeatherType.CLOUDY -> {
                drawCircle(Color(0xFFF8C65B), radius = w * 0.19f, center = Offset(w * 0.36f, h * 0.36f))
                drawCloud(w, h, Color(0xFFE4F2F7))
            }
            WeatherType.OVERCAST -> {
                drawCloud(w, h, Color(0xFFC5DBE3))
            }
            WeatherType.RAIN -> {
                drawCloud(w, h * 0.84f, Color(0xFFCDE6F0))
                listOf(0.34f, 0.52f, 0.70f).forEach { rx ->
                    drawLine(
                        Color(0xFF66D1FF),
                        Offset(w * rx, h * 0.72f),
                        Offset(w * (rx - 0.06f), h * 0.90f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            WeatherType.STORM -> {
                drawCloud(w, h * 0.82f, Color(0xFF9EBBC7))
                val bolt = Path().apply {
                    moveTo(w * 0.54f, h * 0.62f)
                    lineTo(w * 0.42f, h * 0.78f)
                    lineTo(w * 0.54f, h * 0.78f)
                    lineTo(w * 0.46f, h * 0.95f)
                }
                drawPath(bolt, Color(0xFFFFD166), style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            WeatherType.SNOW -> {
                drawCloud(w, h * 0.84f, Color(0xFFDDF3FA))
                listOf(0.34f, 0.52f, 0.70f).forEach { sx ->
                    drawCircle(Color(0xFFFFFFFF), radius = w * 0.055f, center = Offset(w * sx, h * 0.82f))
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(w: Float, h: Float, color: Color) {
    drawCircle(color, radius = w * 0.19f, center = Offset(w * 0.38f, h * 0.52f))
    drawCircle(color, radius = w * 0.23f, center = Offset(w * 0.56f, h * 0.46f))
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.22f, h * 0.48f),
        size = Size(w * 0.58f, h * 0.24f),
        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
    )
}

@Composable
fun LockShieldIcon(color: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.42f),
            size = Size(w * 0.56f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.32f, h * 0.18f),
            size = Size(w * 0.36f, h * 0.44f),
            style = stroke,
        )
    }
}

@Composable
fun DanmakuBubbleIcon(color: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.14f, h * 0.20f),
            size = Size(w * 0.72f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
            style = stroke,
        )
        drawLine(color, Offset(w * 0.28f, h * 0.40f), Offset(w * 0.72f, h * 0.40f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.28f, h * 0.55f), Offset(w * 0.58f, h * 0.55f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun StatsChartIcon(color: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val barW = w * 0.15f
        drawRoundRect(color, Offset(w * 0.18f, h * 0.54f), Size(barW, h * 0.28f), CornerRadius(barW / 2))
        drawRoundRect(color, Offset(w * 0.43f, h * 0.30f), Size(barW, h * 0.52f), CornerRadius(barW / 2))
        drawRoundRect(color, Offset(w * 0.68f, h * 0.18f), Size(barW, h * 0.64f), CornerRadius(barW / 2))
    }
}

@Composable
fun LocationPinIcon(color: Color, size: Dp = 15.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val pin = Path().apply {
            moveTo(w * 0.50f, h * 0.90f)
            cubicTo(w * 0.18f, h * 0.58f, w * 0.18f, h * 0.16f, w * 0.50f, h * 0.14f)
            cubicTo(w * 0.82f, h * 0.16f, w * 0.82f, h * 0.58f, w * 0.50f, h * 0.90f)
            close()
        }
        drawPath(pin, color = color, style = Stroke(width = w * 0.11f))
        drawCircle(color = color, radius = w * 0.11f, center = Offset(w * 0.50f, h * 0.42f))
    }
}
