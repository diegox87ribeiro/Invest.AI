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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.BenchmarkSeries
import com.example.domain.model.PortfolioType
import com.example.domain.model.SimulationScenarioResult
import com.example.ui.components.DataDisclaimerBanner
import com.example.ui.components.FilterChipBar
import com.example.ui.components.FintechCard
import com.example.ui.components.HorizontalBarDistribution
import com.example.ui.components.InteractiveMultiLineChart
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.formatBrl
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.InvestDashboardState
import kotlin.math.roundToInt

@Composable
fun RiskHhiScreen(state: InvestDashboardState) {
    var portfolioScope by remember { mutableStateOf(PortfolioType.REAL) }
    val report = if (portfolioScope == PortfolioType.REAL) state.realHhi else state.simHhi

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FilterChipBar(
                options = listOf(
                    PortfolioType.REAL to "Carteira Real",
                    PortfolioType.SIMULATION to "Carteira Simulação"
                ),
                selected = portfolioScope,
                onSelect = { portfolioScope = it }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard(
                    title = "Índice HHI",
                    value = "${report.hhiScore.toInt()} / 10.000",
                    subtitle = report.classification,
                    icon = Icons.Default.Shield,
                    accentColor = when {
                        report.hhiScore > 2500 -> CoralRed
                        report.hhiScore >= 1500 -> AmberGold
                        else -> EmeraldPrimary
                    },
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Maior Posição",
                    value = report.byAsset.firstOrNull()?.let { "${it.label} (${"%.1f".format(it.weightPercent)}%)" } ?: "0%",
                    subtitle = "Teto prudencial: 20%",
                    icon = Icons.Default.WarningAmber,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            FintechCard {
                Text("Diagnóstico & Alertas de Concentração", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                report.alerts.forEach { alert ->
                    Text("• $alert", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        if (report.byAsset.isNotEmpty()) {
            item {
                FintechCard {
                    Text("Concentração por Ativo (%)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    HorizontalBarDistribution(
                        items = report.byAsset.map { it.label to it.weightPercent },
                        unitSuffix = "%"
                    )
                }
            }

            item {
                FintechCard {
                    Text("Concentração por Setor / Segmento (%)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    HorizontalBarDistribution(
                        items = report.bySector.map { it.label to it.weightPercent },
                        unitSuffix = "%"
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatorScreen(
    state: InvestDashboardState,
    onSimulate: (Double, Double, Int, Double, Double, Boolean, Double) -> SimulationScenarioResult
) {
    val baseCapital = state.simHhi.totalEquityBrl.let { if (it > 0) it else 25000.0 }
    var initialCap by remember(baseCapital) { mutableFloatStateOf(baseCapital.toFloat()) }
    var monthlyContribution by remember { mutableFloatStateOf(1500f) }
    var horizonMonths by remember { mutableFloatStateOf(36f) }
    var annualReturn by remember { mutableFloatStateOf(12.5f) }
    var annualDy by remember { mutableFloatStateOf(8.5f) }
    var reinvestDividends by remember { mutableStateOf(true) }
    var stressShock by remember { mutableFloatStateOf(-18f) }

    val result = remember(initialCap, monthlyContribution, horizonMonths, annualReturn, annualDy, reinvestDividends, stressShock) {
        onSimulate(
            initialCap.toDouble(),
            monthlyContribution.toDouble(),
            horizonMonths.roundToInt(),
            annualReturn.toDouble(),
            annualDy.toDouble(),
            reinvestDividends,
            stressShock.toDouble()
        )
    }

    val normalizedCurve = remember(result) {
        val base = result.projectedCurveMonths.firstOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val sampled = if (result.projectedCurveMonths.size <= 12) {
            result.projectedCurveMonths.map { (it / base) * 100.0 }
        } else {
            val step = (result.projectedCurveMonths.size - 1) / 11.0
            (0..11).map { i ->
                val idx = (i * step).roundToInt().coerceIn(0, result.projectedCurveMonths.lastIndex)
                (result.projectedCurveMonths[idx] / base) * 100.0
            }
        }
        listOf(
            BenchmarkSeries(
                name = "Projeção Simulada",
                currentLabel = formatBrl(result.finalEquityBrl),
                return12mPercent = ((result.finalEquityBrl / base) - 1.0) * 100.0,
                normalizedPoints12m = sampled,
                source = "Simulador Local"
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DataDisclaimerBanner(
                text = "Simulador de Carteira isolado da Carteira Real. Projeta evolução patrimonial, renda passiva e teste de estresse."
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard(
                    title = "Patrimônio Final (${horizonMonths.roundToInt()}m)",
                    value = formatBrl(result.finalEquityBrl),
                    subtitle = "Aportado: ${formatBrl(result.totalContributedBrl)}",
                    icon = Icons.Default.Science,
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Renda Passiva / Mês",
                    value = formatBrl(result.projectedDividendsMonthlyBrl),
                    subtitle = "Estresse (${stressShock.roundToInt()}%): ${formatBrl(result.stressedEquityPessimisticBrl)}",
                    icon = Icons.Default.Shield,
                    accentColor = AmberGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            FintechCard {
                Text("Curva Projetada (Base 100 no início)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                InteractiveMultiLineChart(seriesList = normalizedCurve)
            }
        }

        item {
            FintechCard {
                Text("Parâmetros da Simulação", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Text("Capital Inicial: ${formatBrl(initialCap.toDouble())}", style = MonoValueStyle)
                Slider(value = initialCap, onValueChange = { initialCap = it }, valueRange = 1000f..500000f)

                Text("Aporte Mensal: ${formatBrl(monthlyContribution.toDouble())}", style = MonoValueStyle, color = EmeraldPrimary)
                Slider(value = monthlyContribution, onValueChange = { monthlyContribution = it }, valueRange = 0f..20000f)

                Text("Horizonte: ${horizonMonths.roundToInt()} meses", style = MonoValueStyle, color = ElectricCyan)
                Slider(value = horizonMonths, onValueChange = { horizonMonths = it }, valueRange = 6f..120f)

                Text("Retorno Anual (Cota): ${"%.1f".format(annualReturn)}% a.a.", style = MonoValueStyle)
                Slider(value = annualReturn, onValueChange = { annualReturn = it }, valueRange = 4f..24f)

                Text("Dividend Yield Médio: ${"%.1f".format(annualDy)}% a.a.", style = MonoValueStyle, color = AmberGold)
                Slider(value = annualDy, onValueChange = { annualDy = it }, valueRange = 0f..16f)

                Text("Choque de Estresse Pessimista: ${stressShock.roundToInt()}%", style = MonoValueStyle, color = CoralRed)
                Slider(value = stressShock, onValueChange = { stressShock = it }, valueRange = -45f..-5f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reinvestir dividendos automaticamente", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reinvestDividends, onCheckedChange = { reinvestDividends = it })
                }
            }
        }
    }
}
