package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.AssetClass
import com.example.domain.model.DataComGroup
import com.example.domain.model.MarketAsset
import com.example.ui.components.AssetDetailSheetContent
import com.example.ui.components.DataDisclaimerBanner
import com.example.ui.components.FilterChipBar
import com.example.ui.components.FintechCard
import com.example.ui.components.GroupBadge
import com.example.ui.components.formatBrl
import com.example.ui.components.formatPct
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetClassExplorerScreen(
    assetClass: AssetClass,
    allAssets: List<MarketAsset>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<DataComGroup?>(null) }
    var sortKey by remember { mutableStateOf("DY") }
    var selectedAsset by remember { mutableStateOf<MarketAsset?>(null) }

    val baseAssets = remember(allAssets, assetClass) {
        allAssets.filter { it.assetClass == assetClass }
    }

    val filtered = remember(baseAssets, searchQuery, selectedGroup, sortKey) {
        baseAssets
            .filter { a ->
                val matchesText = searchQuery.isBlank() ||
                    a.ticker.contains(searchQuery, ignoreCase = true) ||
                    a.name.contains(searchQuery, ignoreCase = true) ||
                    a.sector.contains(searchQuery, ignoreCase = true)
                val matchesGroup = selectedGroup == null || a.dataComGroup == selectedGroup
                matchesText && matchesGroup
            }
            .let { list ->
                when (sortKey) {
                    "DY" -> list.sortedByDescending { it.dy12m }
                    "PVP" -> list.sortedBy { it.pvp }
                    "LIQ" -> list.sortedByDescending { it.liquidezDiariaMilhoes }
                    else -> list.sortedBy { it.ticker }
                }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DataDisclaimerBanner(
                text = "Base demonstrativa (${assetClass.label}) referenciada em B3, CVM, BCB e ANBIMA. Os valores exibidos são fictícios para análise estrutural.",
                isWarning = false
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Filtrar por ticker, nome ou setor") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipBar(
                    options = listOf(
                        "DY" to "Maior DY",
                        "PVP" to "Menor P/VP",
                        "LIQ" to "Maior Liquidez",
                        "TICKER" to "A–Z"
                    ),
                    selected = sortKey,
                    onSelect = { sortKey = it }
                )
            }
        }

        if (assetClass == AssetClass.ACOES_BR || assetClass == AssetClass.FII || assetClass == AssetClass.FIAGRO) {
            item {
                FilterChipBar(
                    options = listOf(null to "Todos Grupos") + DataComGroup.entries.map { it to it.code },
                    selected = selectedGroup,
                    onSelect = { selectedGroup = it }
                )
            }
        }

        items(filtered, key = { it.ticker }) { asset ->
            FintechCard(
                modifier = Modifier.clickable { selectedAsset = asset }
            ) {
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
                        Text(asset.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${asset.sector} • ${asset.sourceLabel}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatBrl(asset.price), style = MonoValueStyle)
                        Text(
                            formatPct(asset.changePercent1d),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (asset.changePercent1d >= 0) EmeraldPrimary else CoralRed
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    when (assetClass) {
                        AssetClass.ACOES_BR -> {
                            MetricMini("DY 12m", formatPct(asset.dy12m, false), EmeraldPrimary)
                            MetricMini("P/L", "${"%.1f".format(asset.pl)}x", ElectricCyan)
                            MetricMini("P/VP", "${"%.2f".format(asset.pvp)}x", MaterialTheme.colorScheme.onSurface)
                            MetricMini("ROE", formatPct(asset.roe, false), AmberGold)
                        }
                        AssetClass.FII, AssetClass.FIAGRO -> {
                            MetricMini("DY 12m", formatPct(asset.dy12m, false), EmeraldPrimary)
                            MetricMini("P/VP", "${"%.2f".format(asset.pvp)}x", ElectricCyan)
                            MetricMini("Inadimp.", formatPct(asset.inadimplenciaPercent, false), if (asset.inadimplenciaPercent > 2.5) CoralRed else EmeraldPrimary)
                            MetricMini("Liq/Dia", "R$${"%.1f".format(asset.liquidezDiariaMilhoes)}M", AmberGold)
                        }
                        AssetClass.ETF -> {
                            MetricMini("Índice", asset.indexer, ElectricCyan)
                            MetricMini("Taxa Adm", formatPct(asset.adminFeePercent, false), EmeraldPrimary)
                            MetricMini("Liq/Dia", "R$${"%.0f".format(asset.liquidezDiariaMilhoes)}M", AmberGold)
                        }
                        AssetClass.RENDA_FIXA -> {
                            MetricMini("Indexador", asset.indexer, ElectricCyan)
                            MetricMini("Venc.", asset.maturityDate ?: "-", MaterialTheme.colorScheme.onSurface)
                            MetricMini("FGC", if (asset.fgcCovered) "Sim" else "Soberano/Não", EmeraldPrimary)
                        }
                        AssetClass.CRIPTO -> {}
                    }
                }
            }
        }
    }

    selectedAsset?.let { asset ->
        ModalBottomSheet(onDismissRequest = { selectedAsset = null }) {
            AssetDetailSheetContent(asset)
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = color)
    }
}
