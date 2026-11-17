package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataComGroup
import com.example.ui.components.DataDisclaimerBanner
import com.example.ui.components.FintechCard
import com.example.ui.components.HorizontalBarDistribution
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.InteractiveMultiLineChart
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SecurityGuardBanner
import com.example.ui.components.formatBrl
import com.example.ui.navigation.ALL_INVEST_MODULES
import com.example.ui.navigation.InvestRoutes
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.InvestDashboardState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: InvestDashboardState,
    onNavigate: (String) -> Unit,
    onLoadDemoPortfolio: () -> Unit
) {
    val realHhi = state.realHhi
    val hasRealData = state.realPositions.isNotEmpty()
    val isRealDemo = state.latestRealImport?.isDemoData == true || state.realPositions.any { it.isDemo }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SecurityGuardBanner()
        }

        item {
            if (!hasRealData) {
                FintechCard(borderColor = AmberGold.copy(alpha = 0.5f)) {
                    Text(
                        "Carteira Real sem dados importados",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberGold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Nenhuma posição real foi inventada. Importe sua planilha XLSX ou CSV na aba Carteira, registre posições manualmente ou carregue a carteira demonstrativa fictícia para explorar os gráficos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onNavigate(InvestRoutes.ROUTE_CARTEIRA) },
                            modifier = Modifier.testTag("dashboard_go_import_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Importar XLSX/CSV")
                        }
                        OutlinedButton(
                            onClick = onLoadDemoPortfolio,
                            modifier = Modifier.testTag("dashboard_load_demo_button")
                        ) {
                            Text("Carregar Demo (Fictícia)")
                        }
                    }
                }
            } else if (isRealDemo) {
                DataDisclaimerBanner(
                    text = "A Carteira Real exibe atualmente DADOS FICTÍCIOS DE DEMONSTRAÇÃO (${state.latestRealImport?.fileName}). Importe um CSV/XLSX oficial para substituir.",
                    isWarning = true
                )
            } else {
                DataDisclaimerBanner(
                    text = "Referência da Carteira Real: ${state.latestRealImport?.fileName ?: "Registro Manual"} • ${state.realPositions.size} posições locais."
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiMetricCard(
                    title = "Patrimônio Real",
                    value = formatBrl(realHhi.totalEquityBrl),
                    subtitle = "Caixa: ${formatBrl(realHhi.cashBrl)}",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Concentração HHI",
                    value = "${realHhi.hhiScore.toInt()} pts",
                    subtitle = realHhi.classification,
                    icon = Icons.Default.Shield,
                    accentColor = when {
                        realHhi.hhiScore > 2500 -> CoralRed
                        realHhi.hhiScore >= 1500 -> AmberGold
                        else -> ElectricCyan
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiMetricCard(
                    title = "Carteira Simulação",
                    value = formatBrl(state.simHhi.totalEquityBrl),
                    subtitle = "${state.simPositions.size} ativos simulados (isolados)",
                    icon = Icons.Default.TrendingUp,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Cripto Virtual",
                    value = formatBrl(state.cryptoVirtualPositions.sumOf { it.allocationBrl }.ifZero(500.0)),
                    subtitle = "Agressivo • Sem alavancagem",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = AmberGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            FintechCard {
                Text("Distribuição por Classe (Carteira Real)", style = MaterialTheme.typography.titleMedium)
                Text("Toque em uma fatia para inspecionar peso e classe", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(14.dp))
                InteractiveDonutChart(
                    slices = realHhi.byClass,
                    centerLabel = "Investido",
                    centerValue = formatBrl(realHhi.totalInvestedBrl)
                )
            }
        }

        item {
            FintechCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Benchmarks & Simulação (12m • Base 100)", style = MaterialTheme.typography.titleMedium)
                        Text("Dados demonstrativos comparando Cripto Virtual, BTC, ETH, CDI, Ibovespa e Dólar", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                InteractiveMultiLineChart(seriesList = state.benchmarks)
            }
        }

        item {
            FintechCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mapa Rápido de Datas-Com (G1–G4)", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldPrimary)
                }
                Spacer(Modifier.height(8.dp))
                val groupCounts = DataComGroup.entries.map { g ->
                    val count = state.marketAssets.count { it.dataComGroup == g }.toDouble()
                    g.label to count
                }
                HorizontalBarDistribution(items = groupCounts, unitSuffix = " ativos")
            }
        }

        item {
            FintechCard {
                Text("Todos os 15 Módulos da Plataforma", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ALL_INVEST_MODULES.forEach { mod ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = EmeraldContainer.copy(alpha = 0.35f),
                            modifier = Modifier
                                .testTag("quick_module_${mod.number}")
                                .clickable { onNavigate(mod.route) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(mod.icon, contentDescription = mod.title, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${mod.number}. ${mod.shortTitle}", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        item {
            FintechCard {
                Text("Radar de Riscos & Auditoria HHI", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                realHhi.alerts.forEach { alert ->
                    Text("• $alert", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(vertical = 2.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Última verificação local", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("HHI: ${realHhi.hhiScore.toInt()} / 10.000", style = MonoValueStyle, color = EmeraldPrimary)
                }
            }
        }
    }
}

private fun Double.ifZero(fallback: Double): Double = if (this <= 0.0) fallback else this
