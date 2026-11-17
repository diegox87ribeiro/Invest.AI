package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataComGroup
import com.example.ui.components.DataDisclaimerBanner
import com.example.ui.components.FilterChipBar
import com.example.ui.components.FintechCard
import com.example.ui.components.GroupBadge
import com.example.ui.components.HorizontalBarDistribution
import com.example.ui.components.formatBrl
import com.example.ui.components.formatPct
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.InvestDashboardState

@Composable
fun DividendCalendarScreen(state: InvestDashboardState) {
    var selectedGroup by remember { mutableStateOf<DataComGroup?>(null) }

    val dividendAssets = remember(state.marketAssets) {
        state.marketAssets.filter { it.dataComDay != null }.sortedBy { it.dataComDay }
    }
    val filtered = remember(dividendAssets, selectedGroup) {
        if (selectedGroup == null) dividendAssets
        else dividendAssets.filter { it.dataComGroup == selectedGroup }
    }

    val realPositionsByTicker = remember(state.realPositions) {
        state.realPositions.associateBy { it.ticker }
    }

    val estimatedMonthlyIncome = remember(state.realPositions, state.marketAssets) {
        state.realPositions.sumOf { pos ->
            val asset = state.marketAssets.firstOrNull { it.ticker == pos.ticker }
            if (asset != null && asset.lastDividendBrl > 0) pos.quantity * asset.lastDividendBrl else 0.0
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DataDisclaimerBanner(
                text = "Calendário estratégico por grupos de Data-Com (G1: dias 1–8 • G2: 9–16 • G3: 17–24 • G4: 25–31). Datas demonstrativas sujeitas a confirmação no RI/B3."
            )
        }

        item {
            FintechCard {
                Text("Fluxo de Proventos por Grupo G1–G4", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Provento estimado na Carteira Real: ${formatBrl(estimatedMonthlyIncome)} / ciclo",
                    style = MonoValueStyle,
                    color = EmeraldPrimary
                )
                Spacer(Modifier.height(10.dp))
                val groupCounts = DataComGroup.entries.map { grp ->
                    grp.label to dividendAssets.count { it.dataComGroup == grp }.toDouble()
                }
                HorizontalBarDistribution(items = groupCounts, unitSuffix = " ativos")
            }
        }

        item {
            FilterChipBar(
                options = listOf(null to "Todos (G1–G4)") + DataComGroup.entries.map { it to it.label },
                selected = selectedGroup,
                onSelect = { selectedGroup = it }
            )
        }

        items(filtered, key = { it.ticker }) { asset ->
            val owned = realPositionsByTicker[asset.ticker]
            val projectedBrl = owned?.let { it.quantity * asset.lastDividendBrl } ?: 0.0
            FintechCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(asset.ticker, style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
                            Spacer(Modifier.width(8.dp))
                            GroupBadge(asset.dataComGroup)
                        }
                        Text("${asset.name} • ${asset.assetClass.shortLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "Data-Com: Dia ${asset.dataComDay} • Pgto previsto: Dia ${asset.paymentDay ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatBrl(asset.lastDividendBrl) + "/cota", style = MonoValueStyle)
                        Text("DY 12m: ${formatPct(asset.dy12m, false)}", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                }
                if (owned != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Na sua Carteira Real (${"%.0f".format(owned.quantity)} cotas): projeção de ${formatBrl(projectedBrl)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}
