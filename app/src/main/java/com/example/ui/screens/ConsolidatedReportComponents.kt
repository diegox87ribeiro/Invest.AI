package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.SavedAiRecommendationEntity
import com.example.domain.model.AiAgentRole
import com.example.domain.model.AiConclusion
import com.example.domain.model.ConsolidatedExecutiveReport
import com.example.ui.components.ConclusionBadge
import com.example.ui.components.FintechCard
import com.example.ui.components.formatBrl
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.InvestDashboardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AgentConsensusOverviewCard(
    report: ConsolidatedExecutiveReport,
    consentGranted: Boolean
) {
    FintechCard(
        modifier = Modifier.testTag("consolidated_consensus_card"),
        borderColor = EmeraldPrimary.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Consenso Executivo dos Agentes",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldPrimary
                )
                Text(
                    "Escopo: ${report.filterTicker ?: "Visão Geral"} • Cobertura ${report.coveredAgentsCount}/3 agentes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            val consensus = report.consensusConclusion
            if (consensus != null) {
                ConclusionBadge(conclusion = consensus)
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberContainer
                ) {
                    Text(
                        "Pendente",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Confiança Média Consolidada", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text("${report.averageConfidence}%", style = MonoValueStyle, color = ElectricCyan)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (report.averageConfidence / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = EmeraldPrimary,
            trackColor = CardElevated
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentStatusPill(
                label = "Analista",
                insight = report.analystInsight,
                accent = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            AgentStatusPill(
                label = "Gestor",
                insight = report.gestorInsight,
                accent = VioletAccent,
                modifier = Modifier.weight(1f)
            )
            AgentStatusPill(
                label = "Auditor",
                insight = report.auditorInsight,
                accent = AmberGold,
                modifier = Modifier.weight(1f)
            )
        }

        if (report.hasConsensusDivergence) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Divergência identificada entre agentes: critério prudencial aplicado ao consenso.",
                style = MaterialTheme.typography.labelSmall,
                color = AmberGold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (consentGranted) {
                "Privacidade: resumo agregado HHI autorizado nos pareceres da IA."
            } else {
                "Privacidade: planilhas financeiras pessoais isoladas localmente."
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (consentGranted) EmeraldPrimary else TextSecondary
        )
    }
}

@Composable
private fun AgentStatusPill(
    label: String,
    insight: SavedAiRecommendationEntity?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardElevated)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
        Text(
            text = if (insight != null) "${insight.conclusion} (${insight.confidence}%)" else "Sem parecer",
            style = MaterialTheme.typography.labelMedium,
            color = if (insight != null) MaterialTheme.colorScheme.onSurface else TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun TripartiteAgentInsightsSection(
    report: ConsolidatedExecutiveReport,
    selectedRoleFilter: AiAgentRole?
) {
    val panels = listOf(
        AgentPanelSpec(
            role = AiAgentRole.ANALISTA,
            title = "Analista • Fundamentos & Técnica",
            icon = Icons.Default.Analytics,
            accent = ElectricCyan,
            insight = report.analystInsight,
            emptyHint = "Sem parecer do Analista para este filtro. Execute a consolidação acima ou use o módulo Agentes IA."
        ),
        AgentPanelSpec(
            role = AiAgentRole.GESTOR,
            title = "Gestor • Alocação & Risco HHI",
            icon = Icons.Default.PieChart,
            accent = VioletAccent,
            insight = report.gestorInsight,
            emptyHint = "Sem parecer do Gestor para este filtro. Execute a consolidação para avaliar peso e diversificação."
        ),
        AgentPanelSpec(
            role = AiAgentRole.AUDITOR,
            title = "Auditor • Dados, Fontes & Consistência",
            icon = Icons.Default.FactCheck,
            accent = AmberGold,
            insight = report.auditorInsight,
            emptyHint = "Sem parecer do Auditor para este filtro. Execute a consolidação para auditar fontes e cotações."
        )
    ).filter { selectedRoleFilter == null || it.role == selectedRoleFilter }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wideLayout = maxWidth >= 840.dp && panels.size > 1
        if (wideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                panels.forEach { spec ->
                    AgentInsightDetailCard(
                        spec = spec,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                panels.forEach { spec ->
                    AgentInsightDetailCard(
                        spec = spec,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private data class AgentPanelSpec(
    val role: AiAgentRole,
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val insight: SavedAiRecommendationEntity?,
    val emptyHint: String
)

@Composable
private fun AgentInsightDetailCard(
    spec: AgentPanelSpec,
    modifier: Modifier = Modifier
) {
    val insight = spec.insight
    val fmt = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
    FintechCard(
        modifier = modifier.testTag("agent_panel_${spec.role.name.lowercase()}"),
        borderColor = spec.accent.copy(alpha = if (insight != null) 0.5f else 0.2f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(spec.icon, contentDescription = spec.title, tint = spec.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(spec.title, style = MaterialTheme.typography.titleSmall, color = spec.accent)
                    if (insight != null) {
                        Text(
                            "${insight.ticker} • Confiança ${insight.confidence}% • ${fmt.format(Date(insight.createdAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            if (insight != null) {
                val conclusion = AiConclusion.entries.firstOrNull {
                    it.label.equals(insight.conclusion, ignoreCase = true)
                } ?: AiConclusion.AGUARDAR
                ConclusionBadge(conclusion = conclusion)
            }
        }

        Spacer(Modifier.height(8.dp))
        if (insight == null) {
            Text(spec.emptyHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        } else {
            Text(insight.thesis, style = MaterialTheme.typography.bodySmall)
            if (insight.indicatorsSummary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Indicadores: ${insight.indicatorsSummary}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
            if (insight.scenariosSummary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(insight.scenariosSummary, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            if (insight.sourceLabel.isNotBlank() || insight.dataBase.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = SubtleBorder)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Fonte: ${insight.sourceLabel.ifBlank { "Local" }} • Base: ${insight.dataBase.ifBlank { "Atual" }}" +
                        if (insight.isDemoPlaceholder) " [DEMO]" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ConsolidatedRisksChecklistCard(
    report: ConsolidatedExecutiveReport
) {
    FintechCard(
        modifier = Modifier.testTag("consolidated_risks_card"),
        borderColor = if (report.missingPriceCount > 0) AmberGold.copy(alpha = 0.5f) else SubtleBorder
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GppMaybe, contentDescription = "Auditoria de Riscos", tint = AmberGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Auditoria Consolidada de Riscos & HHI", style = MaterialTheme.typography.titleMedium, color = AmberGold)
        }
        Spacer(Modifier.height(8.dp))
        if (report.combinedRisks.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nenhum alerta crítico identificado até o momento.", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                report.combinedRisks.forEach { risk ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (risk.contains("Integridade")) CoralContainer.copy(alpha = 0.45f)
                                else CardElevated
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text("• $risk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutivePortfolioSummaryCard(state: InvestDashboardState) {
    FintechCard(modifier = Modifier.testTag("executive_portfolio_summary_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Resumo Patrimonial (Real vs Simulação)", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldContainer.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Text("Carteira Real", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                Text(formatBrl(state.realHhi.totalEquityBrl), style = MonoValueStyle)
                Text(
                    "${state.realPositions.size} ativos • HHI ${state.realHhi.hhiScore.toInt()} pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardElevated)
                    .padding(10.dp)
            ) {
                Text("Simulação", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                Text(formatBrl(state.simHhi.totalEquityBrl), style = MonoValueStyle)
                Text(
                    "${state.simPositions.size} ativos • HHI ${state.simHhi.hhiScore.toInt()} pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Referência Real: ${state.latestRealImport?.fileName ?: "Nenhuma planilha importada"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
