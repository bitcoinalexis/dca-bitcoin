package com.angylabs.mydcabtconor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angylabs.mydcabtconor.ui.theme.GoldPrimary
import com.angylabs.mydcabtconor.ui.theme.NavySurface
import com.angylabs.mydcabtconor.ui.theme.TextDim
import com.angylabs.mydcabtconor.ui.theme.TextLight

@Composable
fun LineAreaChart(
    title: String,
    values: List<Double>,
    color: Color = GoldPrimary,
    height: Int = 160
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NavySurface)
            .padding(14.dp)
    ) {
        Text(title, color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (values.isEmpty()) {
            Text("Sin datos", color = TextDim, fontSize = 12.sp)
            return@Column
        }
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).coerceAtLeast(0.0000001)
        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val w = size.width
            val h = size.height
            val step = if (values.size > 1) w / (values.size - 1) else 0f
            val points = values.mapIndexed { i, v ->
                val x = i * step
                val y = h - (((v - minV) / range).toFloat() * h)
                Offset(x, y)
            }
            val area = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.05f)))
            )
            val stroke = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(stroke, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
            points.forEach {
                drawCircle(color = color, radius = 4f, center = it)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Min ${"%.4f".format(minV)}  |  Max ${"%.4f".format(maxV)}",
            color = TextDim,
            fontSize = 10.sp
        )
    }
}

@Composable
fun BarChart(
    title: String,
    values: List<Double>,
    color: Color = GoldPrimary,
    height: Int = 160
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NavySurface)
            .padding(14.dp)
    ) {
        Text(title, color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (values.isEmpty()) {
            Text("Sin datos", color = TextDim, fontSize = 12.sp)
            return@Column
        }
        val maxV = values.max().coerceAtLeast(0.0000001)
        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val w = size.width
            val h = size.height
            val gap = 6f
            val bw = ((w - gap * (values.size + 1)) / values.size).coerceAtLeast(2f)
            values.forEachIndexed { i, v ->
                val bh = ((v / maxV).toFloat() * h)
                val x = gap + i * (bw + gap)
                drawRect(
                    brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))),
                    topLeft = Offset(x, h - bh),
                    size = Size(bw, bh)
                )
            }
        }
    }
}
