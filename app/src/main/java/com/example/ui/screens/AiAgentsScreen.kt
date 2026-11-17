package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.AiAgentRole
import com.example.domain.model.AiRecommendation
import com.example.ui.components.ConclusionBadge
import com.example.ui.components.DataDisclaimerBanner
import com.example.ui.components.FilterChipBar
import com.example.ui.components.FintechCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiAgentUiState
import com.example.ui.viewmodel.AiAgentsViewModel
import com.example.ui.viewmodel.AnalysisFocus
import com.example.ui.viewmodel.AnalysisHorizon
import com.example.ui.viewmodel.InvestDashboardState

@Composable
fun AiAgentsScreen(
    state: InvestDashboardState,
    onSetConsent: (Boolean) -> Unit,
    aiAgentsViewModel: AiAgentsViewModel = viewModel()
) {
    val promptInput by aiAgentsViewModel.promptInput.collectAsStateWithLifecycle()
    val aiUiState by aiAgentsViewModel.uiState.collectAsStateWithLifecycle()
    val savedHistory by aiAgentsViewModel.savedHistory.collectAsStateWithLifecycle()

    val selectedAsset = state.marketAssets.firstOrNull { it.ticker == promptInput.selectedTicker }
        ?: state.marketAssets.first()
    val consentGranted = state.preferences.aiPortfolioConsentGranted

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (aiAgentsViewModel.isLiveGeminiEnabled) {
                DataDisclaimerBanner(
                    text = "Gemini 3.8 Flash conectado via Firebase AI Logic + App Check. Prompts estruturados retornam JSON validado; dados pessoais da carteira permanecem isolados até autorização explícita.",
                    isWarning = false
                )
            } else {
                DataDisclaimerBanner(
                    text = "Aviso de IA: recursos ao vivo com Gemini no Android exigem Firebase AI Logic habilitado.",
                    isWarning = true
                )
            }
        }

        item {
            FintechCard(borderColor = ElectricCyan.copy(alpha = 0.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Privacidade", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Privacidade & Dados Pessoais na IA", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Autorizar explicitamente o envio de resumo HHI/peso da minha carteira pessoal ao Gemini",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = consentGranted,
                        onCheckedChange = onSetConsent,
                        modifier = Modifier.testTag("ai_consent_switch")
                    )
                }
                Text(
                    if (consentGranted) "Autorização explícita ativa para contexto de HHI/peso."
                    else "Planilhas pessoais isoladas localmente (não incluídas no prompt).",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (consentGranted) EmeraldPrimary else AmberGold
                )
            }
        }

        item {
            FintechCard {
                Text("Construtor de Prompt Estruturado", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Text("1. Agente Especializado", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                Spacer(Modifier.height(4.dp))
                FilterChipBar(
                    options = AiAgentRole.entries.map { it to it.title },
                    selected = promptInput.selectedRole,
                    onSelect = aiAgentsViewModel::selectRole
                )
                Text(promptInput.selectedRole.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                Spacer(Modifier.height(10.dp))
                Text("2. Ativo Alvo", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                Spacer(Modifier.height(4.dp))
                FilterChipBar(
                    options = state.marketAssets.take(10).map { it.ticker to it.ticker },
                    selected = promptInput.selectedTicker,
                    onSelect = aiAgentsViewModel::selectTicker
                )

                Spacer(Modifier.height(10.dp))
                Text("3. Horizonte da Análise", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                Spacer(Modifier.height(4.dp))
                FilterChipBar(
                    options = AnalysisHorizon.entries.map { it to it.label },
                    selected = promptInput.horizon,
                    onSelect = aiAgentsViewModel::selectHorizon
                )

                Spacer(Modifier.height(10.dp))
                Text("4. Foco Estruturado", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                Spacer(Modifier.height(4.dp))
                FilterChipBar(
                    options = AnalysisFocus.entries.map { it to it.label },
                    selected = promptInput.focus,
                    onSelect = aiAgentsViewModel::selectFocus
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = promptInput.customQuestion,
                    onValueChange = aiAgentsViewModel::updateCustomQuestion,
                    label = { Text("5. Pergunta ou critério específico (opcional)") },
                    modifier = Modifier.fillMaxWidth().testTag("ai_custom_question_input")
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = aiAgentsViewModel::togglePromptPreview,
                        modifier = Modifier.testTag("toggle_prompt_preview_button")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (promptInput.showPromptPreview) "Ocultar Prompt Estruturado" else "Visualizar Prompt Estruturado")
                    }
                    if (aiUiState !is AiAgentUiState.Idle) {
                        TextButton(onClick = aiAgentsViewModel::resetResult) {
                            Text("Limpar Resultado")
                        }
                    }
                }

                AnimatedVisibility(visible = promptInput.showPromptPreview) {
                    FintechCard(borderColor = ElectricCyan.copy(alpha = 0.35f)) {
                        Text(
                            text = aiAgentsViewModel.buildStructuredPromptPreview(
                                asset = selectedAsset,
                                hhiReport = state.realHhi,
                                includePersonalPortfolio = consentGranted
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        aiAgentsViewModel.submitStructuredAnalysis(
                            asset = selectedAsset,
                            hhiReport = state.realHhi,
                            includePersonalPortfolio = consentGranted
                        )
                    },
                    enabled = aiUiState !is AiAgentUiState.Loading,
                    modifier = Modifier.fillMaxWidth().testTag("run_ai_agent_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar Prompt ao ${promptInput.selectedRole.title} (${selectedAsset.ticker})")
                }
            }
        }

        when (val ui = aiUiState) {
            is AiAgentUiState.Idle -> {
                item {
                    FintechCard {
                        Text(
                            "Configure o agente, ativo, horizonte e foco acima e toque em Enviar Prompt para receber o parecer estruturado do Gemini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            is AiAgentUiState.Loading -> {
                item {
                    FintechCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = EmeraldPrimary)
                            Spacer(Modifier.width(12.dp))
                            Text("Consultando Gemini 3.8 Flash e validando esquema JSON...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            is AiAgentUiState.Error -> {
                item {
                    FintechCard(borderColor = CoralRed) {
                        Text("Falha na Consulta ao Agente Gemini", style = MaterialTheme.typography.titleSmall, color = CoralRed)
                        Spacer(Modifier.height(4.dp))
                        Text(ui.message, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            aiAgentsViewModel.submitStructuredAnalysis(selectedAsset, state.realHhi, consentGranted)
                        }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            }
            is AiAgentUiState.Success -> {
                item {
                    RecommendationStructuredCard(rec = ui.recommendation)
                }
            }
        }

        if (savedHistory.isNotEmpty()) {
            item {
                FintechCard {
                    Text("Histórico Recente de Pareceres", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    savedHistory.take(5).forEach { saved ->
                        Text(
                            "• ${saved.ticker} (${saved.agentRole}) -> ${saved.conclusion} • Confiança ${saved.confidence}%",
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

@Composable
private fun RecommendationStructuredCard(rec: AiRecommendation) {
    FintechCard(borderColor = EmeraldPrimary.copy(alpha = 0.45f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${rec.agentRole.title} • ${rec.ticker}", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
                Text("Confiança: ${rec.nivelConfianca}% • Data-base: ${rec.dataBase}", style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = TextSecondary)
            }
            ConclusionBadge(rec.conclusao)
        }

        Spacer(Modifier.height(10.dp))
        SectionField("1. Tese de Investimento", rec.tese)
        SectionField("2. Dados Utilizados e Data-Base", "${rec.dadosUtilizados} (Base: ${rec.dataBase})")
        SectionField("3. Fonte Consultada", rec.fonteConsultada)

        Text("4. Indicadores Relevantes", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
        rec.indicadoresRelevantes.forEach { (k, v) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(k, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(v, style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize))
            }
        }
        Spacer(Modifier.height(8.dp))

        Text("5. Riscos Identificados", style = MaterialTheme.typography.labelLarge, color = CoralRed)
        rec.riscosIdentificados.forEach { r ->
            Text("• $r", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.height(8.dp))

        Text("6. Cenários (Otimista / Base / Pessimista)", style = MaterialTheme.typography.labelLarge, color = AmberGold)
        Text("• Otimista: ${rec.cenarioOtimista}", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
        Text("• Base: ${rec.cenarioBase}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Text("• Pessimista: ${rec.cenarioPessimista}", style = MaterialTheme.typography.bodySmall, color = CoralRed)

        Spacer(Modifier.height(8.dp))
        Text(
            "7. Nível de Confiança: ${rec.nivelConfianca}%  |  8. Conclusão: ${rec.conclusao.label}",
            style = MonoValueStyle,
            color = EmeraldPrimary
        )
    }
}

@Composable
private fun SectionField(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
}
