package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.domain.model.AllocationSlice
import com.example.domain.model.BenchmarkSeries
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ChartColors
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.min

@Composable
fun InteractiveDonutChart(
    slices: List<AllocationSlice>,
    centerLabel: String,
    centerValue: String,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember(slices) { mutableIntStateOf(0) }

    if (slices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sem posições alocadas para exibir distribuição.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        return
    }

    val total = slices.sumOf { it.valueBrl }.coerceAtLeast(1.0)
    val selected = slices.getOrNull(selectedIdx)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(164.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            var angle = Math.toDegrees(atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble()))
                            angle = (angle + 90.0 + 360.0) % 360.0
                            var acc = 0.0
                            slices.forEachIndexed { idx, slice ->
                                val sweep = (slice.valueBrl / total) * 360.0
                                if (angle >= acc && angle <= acc + sweep) {
                                    selectedIdx = idx
                                    return@detectTapGestures
                                }
                                acc += sweep
                            }
                        }
                    }
            ) {
                val diameter = min(size.width, size.height) - 20f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                var startAngle = -90f
                slices.forEachIndexed { idx, slice ->
                    val sweep = ((slice.valueBrl / total) * 360.0).toFloat()
                    val color = ChartColors[idx % ChartColors.size]
                    val strokeW = if (idx == selectedIdx) 32f else 22f
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(2f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selected?.label ?: centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = selected?.let { "${"%.1f".format(it.weightPercent)}%" } ?: centerValue,
                    style = MonoValueStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.take(6).forEachIndexed { idx, slice ->
                val color = ChartColors[idx % ChartColors.size]
                val isSel = idx == selectedIdx
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) color.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { selectedIdx = idx }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(slice.label, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "${"%.1f".format(slice.weightPercent)}%",
                        style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                        color = if (isSel) color else TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveMultiLineChart(
    seriesList: List<BenchmarkSeries>,
    modifier: Modifier = Modifier
) {
    var selectedSeriesIdx by remember { mutableIntStateOf(0) }
    var inspectedMonthIdx by remember { mutableIntStateOf(11) }

    if (seriesList.isEmpty()) return

    val allValues = seriesList.flatMap { it.normalizedPoints12m }
    val minVal = (allValues.minOrNull() ?: 95.0) - 2.0
    val maxVal = (allValues.maxOrNull() ?: 135.0) + 2.0
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        val activeSeries = seriesList.getOrNull(selectedSeriesIdx)
        val monthPoint = activeSeries?.normalizedPoints12m?.getOrNull(inspectedMonthIdx) ?: 100.0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${activeSeries?.name ?: ""} • Mês ${inspectedMonthIdx + 1}/12",
                style = MaterialTheme.typography.labelLarge,
                color = ChartColors[selectedSeriesIdx % ChartColors.size]
            )
            Text(
                text = "Base 100 -> ${"%.1f".format(monthPoint)} (${formatPct(monthPoint - 100.0)})",
                style = MonoValueStyle,
                color = if (monthPoint >= 100.0) EmeraldPrimary else CoralRed
            )
        }

        Spacer(Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(seriesList) {
                    detectTapGestures { offset ->
                        val count = seriesList.first().normalizedPoints12m.size.coerceAtLeast(2)
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        inspectedMonthIdx = (ratio * (count - 1)).toInt().coerceIn(0, count - 1)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val padV = 12f
            val usableH = h - padV * 2

            // Linhas de grade horizontais
            for (step in 0..3) {
                val y = padV + (usableH * step / 3f)
                drawLine(
                    color = SubtleBorder,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }

            // Linha vertical do mês selecionado
            val count = seriesList.first().normalizedPoints12m.size.coerceAtLeast(2)
            val selX = (inspectedMonthIdx.toFloat() / (count - 1)) * w
            drawLine(
                color = Color.White.copy(alpha = 0.28f),
                start = Offset(selX, 0f),
                end = Offset(selX, h),
                strokeWidth = 2f
            )

            seriesList.forEachIndexed { idx, s ->
                val color = ChartColors[idx % ChartColors.size]
                val pts = s.normalizedPoints12m
                if (pts.size < 2) return@forEachIndexed
                val path = Path()
                pts.forEachIndexed { i, v ->
                    val x = (i.toFloat() / (pts.size - 1)) * w
                    val y = padV + usableH * (1f - ((v - minVal) / range).toFloat())
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = if (idx == selectedSeriesIdx) color else color.copy(alpha = 0.45f),
                    style = Stroke(width = if (idx == selectedSeriesIdx) 5f else 2.5f, cap = StrokeCap.Round)
                )
                val dotVal = pts.getOrNull(inspectedMonthIdx) ?: pts.last()
                val dotY = padV + usableH * (1f - ((dotVal - minVal) / range).toFloat())
                drawCircle(
                    color = color,
                    radius = if (idx == selectedSeriesIdx) 8f else 4f,
                    center = Offset(selX, dotY)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            seriesList.forEachIndexed { idx, s ->
                val color = ChartColors[idx % ChartColors.size]
                val isSelected = idx == selectedSeriesIdx
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) color.copy(alpha = 0.18f) else Color.Transparent,
                    modifier = Modifier.clickable { selectedSeriesIdx = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${s.name} (${s.currentLabel})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) color else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalBarDistribution(
    items: List<Pair<String, Double>>,
    unitSuffix: String = "%",
    accentColor: Color = EmeraldPrimary
) {
    val maxVal = (items.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { idx, (label, value) ->
            val ratio = (value / maxVal).toFloat().coerceIn(0f, 1f)
            val barColor = if (unitSuffix == "%" && value > 20.0) AmberGold else ChartColors[idx % ChartColors.size]
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${"%.1f".format(value)}$unitSuffix",
                        style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                        color = barColor
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SubtleBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
fun TechnicalPriceChart(
    prices: List<Double>,
    mm9: Double,
    mm21: Double,
    support: Double,
    resistance: Double,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return
    val all = prices + listOf(mm9, mm21, support, resistance)
    val minP = (all.minOrNull() ?: 0.0) * 0.985
    val maxP = (all.maxOrNull() ?: 1.0) * 1.015
    val range = (maxP - minP).coerceAtLeast(0.01)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val w = size.width
        val h = size.height

        fun yFor(p: Double): Float = h * (1f - ((p - minP) / range).toFloat())

        // Suporte e Resistência
        drawLine(
            color = EmeraldPrimary.copy(alpha = 0.55f),
            start = Offset(0f, yFor(support)),
            end = Offset(w, yFor(support)),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )
        drawLine(
            color = CoralRed.copy(alpha = 0.55f),
            start = Offset(0f, yFor(resistance)),
            end = Offset(w, yFor(resistance)),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )

        // Médias móveis
        drawLine(
            color = ElectricCyan.copy(alpha = 0.7f),
            start = Offset(0f, yFor(mm9)),
            end = Offset(w, yFor(mm9)),
            strokeWidth = 2.5f
        )
        drawLine(
            color = AmberGold.copy(alpha = 0.7f),
            start = Offset(0f, yFor(mm21)),
            end = Offset(w, yFor(mm21)),
            strokeWidth = 2.5f
        )

        // Preço
        val path = Path()
        val areaPath = Path()
        prices.forEachIndexed { idx, price ->
            val x = (idx.toFloat() / (prices.size - 1)) * w
            val y = yFor(price)
            if (idx == 0) {
                path.moveTo(x, y)
                areaPath.moveTo(x, h)
                areaPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                areaPath.lineTo(x, y)
            }
            if (idx == prices.lastIndex) {
                areaPath.lineTo(x, h)
                areaPath.close()
            }
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                listOf(EmeraldPrimary.copy(alpha = 0.25f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = EmeraldPrimary,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )
    }
}
