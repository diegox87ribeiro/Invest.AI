package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ImportHistoryEntity
import com.example.domain.model.AiAgentRole
import com.example.domain.model.ConsolidatedExecutiveReport
import com.example.domain.model.ConsolidatedReportAggregator
import com.example.domain.model.HhiRiskReport
import com.example.ui.components.DataDisclaimerBanner
import com.example.ui.components.FilterChipBar
import com.example.ui.components.FintechCard
import com.example.ui.components.formatBrl
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiAgentsViewModel
import com.example.ui.viewmodel.CloudReportsUiState
import com.example.ui.viewmodel.ConsolidatedRunState
import com.example.ui.viewmodel.InvestDashboardState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReportsAndArchitectureScreen(
    state: InvestDashboardState,
    reportText: String,
    onSaveFutureIntegrations: (String, String) -> Unit,
    aiAgentsViewModel: AiAgentsViewModel = viewModel(),
    cloudReportsUiState: CloudReportsUiState = CloudReportsUiState.Success(emptyList()),
    isCloudSyncing: Boolean = false,
    onSyncConsolidatedReport: (ConsolidatedExecutiveReport, HhiRiskReport, ImportHistoryEntity?, Int) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val savedHistory by aiAgentsViewModel.savedHistory.collectAsStateWithLifecycle()
    val consolidatedRunState by aiAgentsViewModel.consolidatedRunState.collectAsStateWithLifecycle()

    val effectiveSavedInsights = remember(savedHistory, state.savedAiRecommendations) {
        if (savedHistory.isNotEmpty()) savedHistory else state.savedAiRecommendations
    }

    var selectedTickerToRun by remember(state.marketAssets) {
        mutableStateOf(
            state.realPositions.firstOrNull { rp ->
                state.marketAssets.any { it.ticker == rp.ticker }
            }?.ticker ?: state.marketAssets.first().ticker
        )
    }
    var tickerFilter by remember { mutableStateOf<String?>(null) }
    var selectedRoleFilter by remember { mutableStateOf<AiAgentRole?>(null) }
    var showRawTextPreview by remember { mutableStateOf(false) }

    var supabaseEndpoint by remember(state.preferences.supabaseEndpointConfigured) {
        mutableStateOf(state.preferences.supabaseEndpointConfigured)
    }
    var githubRepo by remember(state.preferences.githubRepoConfigured) {
        mutableStateOf(state.preferences.githubRepoConfigured)
    }

    val consolidatedReport = remember(effectiveSavedInsights, state.realPositions, state.realHhi, tickerFilter) {
        ConsolidatedReportAggregator.aggregate(
            savedInsights = effectiveSavedInsights,
            realPositions = state.realPositions,
            realHhi = state.realHhi,
            tickerFilter = tickerFilter
        )
    }

    val availableFilterTickers = remember(effectiveSavedInsights, state.realPositions) {
        (effectiveSavedInsights.map { it.ticker } + state.realPositions.map { it.ticker })
            .distinct()
            .take(8)
    }

    val targetAsset = remember(selectedTickerToRun, state.marketAssets) {
        state.marketAssets.firstOrNull { it.ticker == selectedTickerToRun } ?: state.marketAssets.first()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(reportText.toByteArray(Charsets.UTF_8))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("reports_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DataDisclaimerBanner(
                text = "Painel Consolidado de Relatórios Gerenciais: agrega pareceres dos agentes Analista, Gestor e Auditor com auditoria HHI local e backup autenticado no Firebase Firestore.",
                isWarning = false
            )
        }

        item {
            FintechCard(borderColor = EmeraldPrimary.copy(alpha = 0.4f)) {
                Text("Orquestração Consolidada (3 Agentes)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Selecione um ativo para consultar Analista, Gestor e Auditor em paralelo e consolidar no painel gerencial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                FilterChipBar(
                    options = state.marketAssets.take(8).map { it.ticker to it.ticker },
                    selected = selectedTickerToRun,
                    onSelect = { selectedTickerToRun = it }
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        tickerFilter = targetAsset.ticker
                        aiAgentsViewModel.runConsolidatedThreeAgents(
                            asset = targetAsset,
                            hhiReport = state.realHhi,
                            includePersonalPortfolio = state.preferences.aiPortfolioConsentGranted
                        )
                    },
                    enabled = consolidatedRunState !is ConsolidatedRunState.Loading,
                    modifier = Modifier.fillMaxWidth().testTag("run_consolidated_agents_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
                ) {
                    if (consolidatedRunState is ConsolidatedRunState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = EmeraldOnPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Consolidando Analista, Gestor e Auditor...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Consolidar 3 Agentes (${targetAsset.ticker})")
                    }
                }
                when (val run = consolidatedRunState) {
                    is ConsolidatedRunState.Completed -> {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Painel atualizado para ${run.ticker} (${run.generatedCount} agentes consolidados).",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                    }
                    is ConsolidatedRunState.Error -> {
                        Spacer(Modifier.height(6.dp))
                        Text(run.message, style = MaterialTheme.typography.labelSmall, color = CoralRed)
                    }
                    else -> Unit
                }
            }
        }

        item {
            FintechCard {
                Text("Filtros do Painel Consolidado", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                Spacer(Modifier.height(6.dp))
                FilterChipBar<String?>(
                    options = listOf(null to "Todos") + availableFilterTickers.map { it to it },
                    selected = tickerFilter,
                    onSelect = { tickerFilter = it }
                )
                Spacer(Modifier.height(6.dp))
                FilterChipBar<AiAgentRole?>(
                    options = listOf(
                        null to "Todos Agentes",
                        AiAgentRole.ANALISTA to "Analista",
                        AiAgentRole.GESTOR to "Gestor",
                        AiAgentRole.AUDITOR to "Auditor"
                    ),
                    selected = selectedRoleFilter,
                    onSelect = { selectedRoleFilter = it }
                )
            }
        }

        item {
            AgentConsensusOverviewCard(
                report = consolidatedReport,
                consentGranted = state.preferences.aiPortfolioConsentGranted
            )
        }

        item {
            TripartiteAgentInsightsSection(
                report = consolidatedReport,
                selectedRoleFilter = selectedRoleFilter
            )
        }

        item {
            ConsolidatedRisksChecklistCard(report = consolidatedReport)
        }

        item {
            ExecutivePortfolioSummaryCard(state = state)
        }

        item {
            FintechCard(borderColor = ElectricCyan.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Backup na Nuvem (Firebase Firestore)", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Salve o resumo consolidado atual na sua conta Google autenticada para consulta entre dispositivos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onSyncConsolidatedReport(
                            consolidatedReport,
                            state.realHhi,
                            state.latestRealImport,
                            state.realPositions.size
                        )
                    },
                    enabled = !isCloudSyncing && consolidatedReport.coveredAgentsCount > 0,
                    modifier = Modifier.fillMaxWidth().testTag("sync_report_firestore_button")
                ) {
                    if (isCloudSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Sincronizando com Firestore...")
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Salvar Painel Consolidado no Firestore")
                    }
                }
                Spacer(Modifier.height(8.dp))
                when (cloudReportsUiState) {
                    is CloudReportsUiState.Loading -> {
                        Text("Carregando histórico na nuvem...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    is CloudReportsUiState.Error -> {
                        Text("Sincronização Firestore: ${cloudReportsUiState.message}", style = MaterialTheme.typography.bodySmall, color = AmberGold)
                    }
                    is CloudReportsUiState.Success -> {
                        if (cloudReportsUiState.reports.isEmpty()) {
                            Text("Nenhum relatório consolidado salvo na nuvem ainda.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else {
                            val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                            Text("Relatórios Salvos na Nuvem (${cloudReportsUiState.reports.size})", style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
                            cloudReportsUiState.reports.take(4).forEach { cr ->
                                val dateStr = cr.createdAt?.toDate()?.let { fmt.format(it) } ?: "Recente"
                                Text(
                                    "• ${cr.tickerFilter}: ${cr.consensusConclusion} (${cr.averageConfidence}% • ${cr.coveredAgentsCount}/3 agentes) • ${formatBrl(cr.totalEquityBrl)} • $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            FintechCard {
                Text("Exportação & Compartilhamento (.TXT)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { exportLauncher.launch("invest_ai_relatorio_consolidado.txt") },
                        modifier = Modifier.weight(1f).testTag("export_report_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salvar .TXT")
                    }
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Relatório Gerencial Consolidado INVEST AI")
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Relatório"))
                        },
                        modifier = Modifier.weight(1f).testTag("share_report_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compartilhar")
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { showRawTextPreview = !showRawTextPreview },
                    modifier = Modifier.testTag("toggle_raw_report_button")
                ) {
                    Icon(
                        if (showRawTextPreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (showRawTextPreview) "Ocultar Prévia Completa (.TXT)" else "Visualizar Prévia Completa (.TXT)")
                }
                AnimatedVisibility(visible = showRawTextPreview) {
                    Text(
                        text = reportText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            FintechCard(borderColor = ElectricCyan.copy(alpha = 0.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Arquitetura & Conectores Preparados", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                }
                Spacer(Modifier.height(8.dp))
                Text("• Armazenamento Híbrido: SQLite/Room Local + Firebase Firestore (Google Auth)", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                Text("• ${state.supabaseStatus.providerName}: ${state.supabaseStatus.statusLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("• ${state.githubStatus.providerName}: ${state.githubStatus.statusLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = supabaseEndpoint,
                    onValueChange = { supabaseEndpoint = it },
                    label = { Text("URL Projeto Supabase (preparação futura)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = githubRepo,
                    onValueChange = { githubRepo = it },
                    label = { Text("Repositório GitHub (ex: usuario/invest-ai-backup)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onSaveFutureIntegrations(supabaseEndpoint, githubRepo) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Referências Locais (Sem Ativar Cobrança)")
                }
            }
        }
    }
}
