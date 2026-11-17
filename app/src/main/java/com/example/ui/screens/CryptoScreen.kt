package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.DataDisclaimerBanner
import com.example.ui.components.FintechCard
import com.example.ui.components.InteractiveMultiLineChart
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.formatBrl
import com.example.ui.components.formatPct
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.InvestDashboardState
import kotlin.math.roundToInt

@Composable
fun CryptoScreen(
    state: InvestDashboardState,
    onUpdateAllocations: (Double, Double, Double) -> Unit,
    onResetWallet: () -> Unit
) {
    val positions = state.cryptoVirtualPositions
    val currentBtcPct = positions.firstOrNull { it.ticker == "BTC" }?.allocationBrl?.div(5.0)?.toFloat() ?: 50f
    val currentEthPct = positions.firstOrNull { it.ticker == "ETH" }?.allocationBrl?.div(5.0)?.toFloat() ?: 30f
    val currentSolPct = positions.firstOrNull { it.ticker == "SOL" }?.allocationBrl?.div(5.0)?.toFloat() ?: 20f

    var btcSlider by remember(currentBtcPct) { mutableFloatStateOf(currentBtcPct) }
    var ethSlider by remember(currentEthPct) { mutableFloatStateOf(currentEthPct) }
    var solSlider by remember(currentSolPct) { mutableFloatStateOf(currentSolPct) }

    val totalVirtualNow = positions.sumOf { p ->
        val factor = if (p.entryPriceBrl > 0) p.currentPriceBrl / p.entryPriceBrl else 1.0
        p.allocationBrl * factor
    }.let { if (it <= 0.0) 500.0 else it }
    val virtualReturnPct = ((totalVirtualNow - 500.0) / 500.0) * 100.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DataDisclaimerBanner(
                text = "Carteira Virtual de R$ 500,00 (Perfil Agressivo) • SEM alavancagem • SEM execução de ordens reais. Simulação educativa comparada a benchmarks.",
                isWarning = true
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiMetricCard(
                    title = "Capital Base Virtual",
                    value = "R$ 500,00",
                    subtitle = "Alocação 1x Spot Simulado",
                    icon = Icons.Default.CurrencyBitcoin,
                    accentColor = AmberGold,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Valor Simulado",
                    value = formatBrl(totalVirtualNow),
                    subtitle = "Retorno: ${formatPct(virtualReturnPct)}",
                    icon = Icons.Default.Shield,
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            FintechCard {
                Text("Comparativo 12m: Carteira R$ 500 vs BTC, ETH, CDI, Ibovespa e Dólar", style = MaterialTheme.typography.titleMedium)
                Text("Toque no gráfico ou nas legendas abaixo para inspecionar", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                InteractiveMultiLineChart(seriesList = state.benchmarks)
            }
        }

        item {
            FintechCard {
                Text("Simulador de Rebalanceamento Virtual (R$ 500)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                val sum = (btcSlider + ethSlider + solSlider).coerceAtLeast(1f)
                val normBtc = (btcSlider / sum) * 100.0
                val normEth = (ethSlider / sum) * 100.0
                val normSol = (solSlider / sum) * 100.0

                Text("Bitcoin (BTC): ${normBtc.roundToInt()}% (${formatBrl(500.0 * normBtc / 100.0)})", style = MonoValueStyle, color = AmberGold)
                Slider(value = btcSlider, onValueChange = { btcSlider = it }, valueRange = 0f..100f)

                Text("Ethereum (ETH): ${normEth.roundToInt()}% (${formatBrl(500.0 * normEth / 100.0)})", style = MonoValueStyle, color = ElectricCyan)
                Slider(value = ethSlider, onValueChange = { ethSlider = it }, valueRange = 0f..100f)

                Text("Solana (SOL): ${normSol.roundToInt()}% (${formatBrl(500.0 * normSol / 100.0)})", style = MonoValueStyle, color = EmeraldPrimary)
                Slider(value = solSlider, onValueChange = { solSlider = it }, valueRange = 0f..100f)

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onUpdateAllocations(normBtc, normEth, normSol) },
                        modifier = Modifier.weight(1f).testTag("save_crypto_sim_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
                    ) {
                        Text("Aplicar Simulação")
                    }
                    OutlinedButton(onClick = onResetWallet) {
                        Text("Restaurar Padrão")
                    }
                }
            }
        }

        item {
            FintechCard {
                Text("Posições Virtuais Atuais (Sem Alavancagem)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                positions.forEach { p ->
                    val factor = if (p.entryPriceBrl > 0) p.currentPriceBrl / p.entryPriceBrl else 1.0
                    val currentVal = p.allocationBrl * factor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${p.ticker} • ${p.name}", style = MaterialTheme.typography.bodyMedium)
                            Text("Alocado: ${formatBrl(p.allocationBrl)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(formatBrl(currentVal), style = MonoValueStyle, color = EmeraldPrimary)
                            Text(formatPct((factor - 1.0) * 100.0), style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
