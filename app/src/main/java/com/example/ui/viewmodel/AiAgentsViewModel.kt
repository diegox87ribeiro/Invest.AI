package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.InvestDatabase
import com.example.data.local.SavedAiRecommendationEntity
import com.example.data.repository.AiRepository
import com.example.data.repository.FirebaseAiRepository
import com.example.data.repository.MarketCatalogRepository
import com.example.domain.model.AiAgentRole
import com.example.domain.model.AiRecommendation
import com.example.domain.model.HhiRiskReport
import com.example.domain.model.MarketAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAgentsViewModel"

enum class AnalysisHorizon(val label: String) {
    CURTO("Curto Prazo (1–6m)"),
    MEDIO("Médio Prazo (6–24m)"),
    LONGO("Longo Prazo (Renda / >2a)")
}

enum class AnalysisFocus(val label: String) {
    COMPLETA("Análise Completa"),
    DIVIDENDOS_DATACOM("Dividendos & Datas-Com G1–G4"),
    RISCO_HHI("Risco & Concentração HHI"),
    AUDITORIA_FONTES("Auditoria de Dados & Fontes")
}

data class StructuredPromptInput(
    val selectedRole: AiAgentRole = AiAgentRole.ANALISTA,
    val selectedTicker: String = MarketCatalogRepository.catalogAssets.first().ticker,
    val horizon: AnalysisHorizon = AnalysisHorizon.LONGO,
    val focus: AnalysisFocus = AnalysisFocus.COMPLETA,
    val customQuestion: String = "",
    val showPromptPreview: Boolean = false
)

sealed interface AiAgentUiState {
    data object Idle : AiAgentUiState
    data object Loading : AiAgentUiState
    data class Success(val recommendation: AiRecommendation) : AiAgentUiState
    data class Error(val message: String) : AiAgentUiState
}

sealed interface ConsolidatedRunState {
    data object Idle : ConsolidatedRunState
    data class Loading(val ticker: String) : ConsolidatedRunState
    data class Completed(val ticker: String, val generatedCount: Int) : ConsolidatedRunState
    data class Error(val message: String) : ConsolidatedRunState
}

/**
 * ViewModel dedicado para gerenciar a interação estruturada com os agentes especializados
 * (Analista, Gestor e Auditor) via Firebase AI Logic (Gemini) e consolidar pareceres.
 */
class AiAgentsViewModel(application: Application) : AndroidViewModel(application) {

    private val aiDao = InvestDatabase.getInstance(application).aiRecommendationDao()
    private val aiRepository: AiRepository = FirebaseAiRepository()

    val isLiveGeminiEnabled: Boolean = aiRepository.isLiveGeminiEnabled

    private val _promptInput = MutableStateFlow(StructuredPromptInput())
    val promptInput: StateFlow<StructuredPromptInput> = _promptInput.asStateFlow()

    private val _uiState = MutableStateFlow<AiAgentUiState>(AiAgentUiState.Idle)
    val uiState: StateFlow<AiAgentUiState> = _uiState.asStateFlow()

    private val _consolidatedRunState = MutableStateFlow<ConsolidatedRunState>(ConsolidatedRunState.Idle)
    val consolidatedRunState: StateFlow<ConsolidatedRunState> = _consolidatedRunState.asStateFlow()

    val savedHistory: StateFlow<List<SavedAiRecommendationEntity>> = aiDao.observeSaved()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun selectRole(role: AiAgentRole) {
        _promptInput.update { it.copy(selectedRole = role) }
    }

    fun selectTicker(ticker: String) {
        _promptInput.update { it.copy(selectedTicker = ticker) }
    }

    fun selectHorizon(horizon: AnalysisHorizon) {
        _promptInput.update { it.copy(horizon = horizon) }
    }

    fun selectFocus(focus: AnalysisFocus) {
        _promptInput.update { it.copy(focus = focus) }
    }

    fun updateCustomQuestion(text: String) {
        _promptInput.update { it.copy(customQuestion = text) }
    }

    fun togglePromptPreview() {
        _promptInput.update { it.copy(showPromptPreview = !it.showPromptPreview) }
    }

    fun resetResult() {
        _uiState.value = AiAgentUiState.Idle
    }

    fun clearConsolidatedStatus() {
        _consolidatedRunState.value = ConsolidatedRunState.Idle
    }

    fun buildStructuredPromptPreview(
        asset: MarketAsset,
        hhiReport: HhiRiskReport,
        includePersonalPortfolio: Boolean
    ): String {
        val input = _promptInput.value
        val portfolioLine = if (includePersonalPortfolio && hhiReport.totalInvestedBrl > 0) {
            "Incluído (autorizado): HHI ${hhiReport.hhiScore.toInt()} pts • Patrimônio R$ ${"%.2f".format(hhiReport.totalEquityBrl)}"
        } else {
            "Isolado localmente (planilhas pessoais não enviadas)"
        }
        return """
            [AGENTE]: ${input.selectedRole.title} (${input.selectedRole.subtitle})
            [ATIVO]: ${asset.ticker} — ${asset.name} (${asset.assetClass.label})
            [HORIZONTE]: ${input.horizon.label}
            [FOCO]: ${input.focus.label}
            [FONTES PRIORIZADAS]: B3, CVM, Banco Central, ANBIMA, RI (${asset.sourceLabel})
            [DADOS PESSOAIS]: $portfolioLine
            [PERGUNTA]: ${input.customQuestion.ifBlank { "Gerar tese, indicadores, riscos, cenários e conclusão." }}
        """.trimIndent()
    }

    fun submitStructuredAnalysis(
        asset: MarketAsset,
        hhiReport: HhiRiskReport,
        includePersonalPortfolio: Boolean
    ) {
        val input = _promptInput.value
        viewModelScope.launch {
            _uiState.value = AiAgentUiState.Loading
            try {
                val recommendation = withTimeout(60_000L) {
                    aiRepository.generateRecommendation(
                        role = input.selectedRole,
                        asset = asset,
                        hhiReport = hhiReport,
                        includePersonalPortfolio = includePersonalPortfolio,
                        customPrompt = input.customQuestion,
                        horizonLabel = input.horizon.label,
                        focusLabel = input.focus.label
                    )
                }
                aiDao.insert(recommendation.toSavedEntity())
                _uiState.value = AiAgentUiState.Success(recommendation)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini AI structured analysis timed out", e)
                _uiState.value = AiAgentUiState.Error(
                    "Tempo limite de 60s atingido ao consultar o Gemini. Tente novamente."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _uiState.value = AiAgentUiState.Error(
                    e.localizedMessage ?: "Falha ao executar análise estruturada com Gemini."
                )
            }
        }
    }

    fun runConsolidatedThreeAgents(
        asset: MarketAsset,
        hhiReport: HhiRiskReport,
        includePersonalPortfolio: Boolean
    ) {
        viewModelScope.launch {
            _consolidatedRunState.value = ConsolidatedRunState.Loading(asset.ticker)
            try {
                val results = withTimeout(90_000L) {
                    coroutineScope {
                        AiAgentRole.entries.map { role ->
                            val focus = when (role) {
                                AiAgentRole.ANALISTA -> AnalysisFocus.COMPLETA.label
                                AiAgentRole.GESTOR -> AnalysisFocus.RISCO_HHI.label
                                AiAgentRole.AUDITOR -> AnalysisFocus.AUDITORIA_FONTES.label
                            }
                            async {
                                aiRepository.generateRecommendation(
                                    role = role,
                                    asset = asset,
                                    hhiReport = hhiReport,
                                    includePersonalPortfolio = includePersonalPortfolio,
                                    customPrompt = "Gerar síntese executiva para o Painel Consolidado de Relatórios Gerenciais.",
                                    horizonLabel = AnalysisHorizon.LONGO.label,
                                    focusLabel = focus
                                )
                            }
                        }.awaitAll()
                    }
                }
                results.forEach { rec -> aiDao.insert(rec.toSavedEntity()) }
                _consolidatedRunState.value = ConsolidatedRunState.Completed(asset.ticker, results.size)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Consolidated 3-agent run timed out", e)
                _consolidatedRunState.value = ConsolidatedRunState.Error(
                    "Tempo limite de 90s atingido ao consolidar os 3 agentes."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Consolidated 3-agent run failed", e)
                _consolidatedRunState.value = ConsolidatedRunState.Error(
                    e.localizedMessage ?: "Erro ao consolidar pareceres dos 3 agentes."
                )
            }
        }
    }

    private fun AiRecommendation.toSavedEntity(): SavedAiRecommendationEntity {
        val indicatorsText = indicadoresRelevantes.joinToString(" • ") { "${it.first}: ${it.second}" }
        val risksText = riscosIdentificados.joinToString(" | ")
        val scenariosText = "Otimista: $cenarioOtimista | Base: $cenarioBase | Pessimista: $cenarioPessimista"
        return SavedAiRecommendationEntity(
            agentRole = agentRole.name,
            ticker = ticker,
            conclusion = conclusao.label,
            confidence = nivelConfianca,
            thesis = tese,
            usedPersonalData = usedPersonalPortfolioData,
            sourceLabel = fonteConsultada,
            dataBase = dataBase,
            indicatorsSummary = indicatorsText,
            risksSummary = risksText,
            scenariosSummary = scenariosText,
            isDemoPlaceholder = isDemoPlaceholder
        )
    }
}
