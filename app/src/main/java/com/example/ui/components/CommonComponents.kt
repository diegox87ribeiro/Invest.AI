package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiConclusion
import com.example.domain.model.DataComGroup
import com.example.domain.model.MarketAsset
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

private val ptBrLocale = Locale("pt", "BR")

fun formatBrl(value: Double): String {
    val nf = NumberFormat.getCurrencyInstance(ptBrLocale)
    return nf.format(value)
}

fun formatPct(value: Double, includeSign: Boolean = true): String {
    val sign = if (includeSign && value > 0) "+" else ""
    return "$sign${String.format(ptBrLocale, "%.2f", value)}%"
}

@Composable
fun FintechCard(
    modifier: Modifier = Modifier,
    borderColor: Color = SubtleBorder,
    containerColor: Color = CardElevated,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = EmeraldPrimary,
    modifier: Modifier = Modifier
) {
    FintechCard(modifier = modifier, borderColor = accentColor.copy(alpha = 0.28f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = value,
            style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = accentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DataDisclaimerBanner(
    text: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = if (isWarning) AmberContainer.copy(alpha = 0.7f) else CyanContainer.copy(alpha = 0.65f)
    val fg = if (isWarning) AmberGold else ElectricCyan
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWarning) Icons.Default.WarningAmber else Icons.Default.Info,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SecurityGuardBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = EmeraldContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Segurança ativa",
                tint = EmeraldPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Modo Somente Análise: sem execução de ordens financeiras, sem senhas bancárias e sem chaves no navegador.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ConclusionBadge(conclusion: AiConclusion) {
    val (bg, fg) = when (conclusion) {
        AiConclusion.COMPRAR -> EmeraldContainer to EmeraldPrimary
        AiConclusion.MANTER -> CyanContainer to ElectricCyan
        AiConclusion.AGUARDAR -> AmberContainer to AmberGold
        AiConclusion.VENDER -> CoralContainer to CoralRed
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.6f))
    ) {
        Text(
            text = conclusion.label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GroupBadge(group: DataComGroup?) {
    if (group == null) return
    val color = when (group) {
        DataComGroup.G1 -> EmeraldPrimary
        DataComGroup.G2 -> ElectricCyan
        DataComGroup.G3 -> AmberGold
        DataComGroup.G4 -> Color(0xFFA78BFA)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = group.code,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FilterChipBar(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(key) },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldContainer,
                    selectedLabelColor = EmeraldPrimary
                )
            )
        }
    }
}

@Composable
fun AssetDetailSheetContent(asset: MarketAsset) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.ticker, style = MaterialTheme.typography.headlineSmall, color = EmeraldPrimary)
                Text(asset.name, style = MaterialTheme.typography.titleMedium)
                Text("${asset.assetClass.label} • ${asset.sector}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatBrl(asset.price), style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize))
                Text(
                    formatPct(asset.changePercent1d),
                    style = MonoValueStyle,
                    color = if (asset.changePercent1d >= 0) EmeraldPrimary else CoralRed
                )
            }
        }

        DataDisclaimerBanner(
            text = "Fonte: ${asset.sourceLabel} • Data-base: ${asset.dataBaseDate} • Dados de demonstração para análise.",
            isWarning = false
        )

        FintechCard {
            val metrics = listOf(
                "DY (12m)" to formatPct(asset.dy12m, false),
                "P/VP" to String.format(ptBrLocale, "%.2fx", asset.pvp),
                "Liquidez Diária" to "R$ ${String.format(ptBrLocale, "%.1f", asset.liquidezDiariaMilhoes)}M",
                "Inadimplência" to formatPct(asset.inadimplenciaPercent, false),
                "IFR (14)" to String.format(ptBrLocale, "%.1f", asset.rsi14),
                "Data-Com" to (asset.dataComDay?.let { "Dia $it (${asset.dataComGroup?.code})" } ?: "N/A")
            )
            metrics.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    pair.forEach { (k, v) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(k, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(v, style = MonoValueStyle)
                        }
                    }
                }
            }
        }

        Text("Riscos e Observações", style = MaterialTheme.typography.titleSmall, color = AmberGold)
        Text(asset.riskSummary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
    }
}
