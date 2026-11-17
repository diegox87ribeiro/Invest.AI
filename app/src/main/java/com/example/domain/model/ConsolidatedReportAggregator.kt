package com.example.domain.model

import com.example.data.local.SavedAiRecommendationEntity
import kotlin.math.roundToInt

data class ConsolidatedExecutiveReport(
    val filterTicker: String?,
    val analystInsight: SavedAiRecommendationEntity?,
    val gestorInsight: SavedAiRecommendationEntity?,
    val auditorInsight: SavedAiRecommendationEntity?,
    val coveredAgentsCount: Int,
    val averageConfidence: Int,
    val consensusConclusion: AiConclusion?,
    val hasConsensusDivergence: Boolean,
    val combinedRisks: List<String>,
    val missingPriceCount: Int,
    val filteredHistory: List<SavedAiRecommendationEntity>
)

object ConsolidatedReportAggregator {

    fun aggregate(
        savedInsights: List<SavedAiRecommendationEntity>,
        realPositions: List<PortfolioItemView>,
        realHhi: HhiRiskReport,
        tickerFilter: String? = null
    ): ConsolidatedExecutiveReport {
        val normalizedFilter = tickerFilter?.takeIf { it.isNotBlank() }?.uppercase()
        val scoped = if (normalizedFilter == null) {
            savedInsights
        } else {
            savedInsights.filter { it.ticker.equals(normalizedFilter, ignoreCase = true) }
        }.sortedByDescending { it.createdAt }

        val analyst = scoped.firstOrNull { it.agentRole == AiAgentRole.ANALISTA.name }
        val gestor = scoped.firstOrNull { it.agentRole == AiAgentRole.GESTOR.name }
        val auditor = scoped.firstOrNull { it.agentRole == AiAgentRole.AUDITOR.name }

        val activePerRole = listOfNotNull(analyst, gestor, auditor)
        val coveredCount = activePerRole.size
        val avgConfidence = if (activePerRole.isNotEmpty()) {
            activePerRole.map { it.confidence }.average().roundToInt()
        } else {
            0
        }

        val parsedConclusions = activePerRole.mapNotNull { entity ->
            AiConclusion.entries.firstOrNull {
                it.label.equals(entity.conclusion, ignoreCase = true) ||
                    it.name.equals(entity.conclusion, ignoreCase = true)
            }
        }
        val consensus = computeConsensusConclusion(parsedConclusions)
        val hasDivergence = parsedConclusions.distinct().size > 1

        val missingPriceCount = realPositions.count { !it.hasMarketPrice }
        val combinedRisks = buildList {
            activePerRole.forEach { insight ->
                insight.risksSummary
                    .split("|")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { risk ->
                        val roleTag = AiAgentRole.entries.firstOrNull { it.name == insight.agentRole }?.title ?: insight.agentRole
                        add("[$roleTag • ${insight.ticker}] $risk")
                    }
            }
            realHhi.alerts.forEach { alert ->
                add("[Auditoria HHI] $alert")
            }
            if (missingPriceCount > 0) {
                add("[Integridade de Dados] $missingPriceCount ativo(s) da Carteira Real sem cotação de mercado informada (preços não inventados).")
            }
        }.distinct()

        return ConsolidatedExecutiveReport(
            filterTicker = normalizedFilter,
            analystInsight = analyst,
            gestorInsight = gestor,
            auditorInsight = auditor,
            coveredAgentsCount = coveredCount,
            averageConfidence = avgConfidence,
            consensusConclusion = consensus,
            hasConsensusDivergence = hasDivergence,
            combinedRisks = combinedRisks,
            missingPriceCount = missingPriceCount,
            filteredHistory = scoped
        )
    }

    private fun computeConsensusConclusion(conclusions: List<AiConclusion>): AiConclusion? {
        if (conclusions.isEmpty()) return null
        val counts = conclusions.groupingBy { it }.eachCount()
        val maxFreq = counts.values.maxOrNull() ?: return null
        val tied = counts.filterValues { it == maxFreq }.keys
        if (tied.size == 1) return tied.first()
        // Critério prudencial em empate: Aguardar > Manter > Comprar > Vender
        val priority = listOf(AiConclusion.AGUARDAR, AiConclusion.MANTER, AiConclusion.VENDER, AiConclusion.COMPRAR)
        return priority.firstOrNull { it in tied } ?: conclusions.first()
    }
}
