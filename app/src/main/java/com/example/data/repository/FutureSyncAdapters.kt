package com.example.data.repository

import com.example.data.local.ImportHistoryEntity
import com.example.data.local.SavedAiRecommendationEntity
import com.example.domain.model.AiAgentRole
import com.example.domain.model.ConsolidatedReportAggregator
import com.example.domain.model.HhiRiskReport
import com.example.domain.model.PortfolioItemView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IntegrationAdapterStatus(
    val providerName: String,
    val statusLabel: String,
    val isActive: Boolean,
    val notes: String
)

/**
 * Contrato preparado para futura sincronização com Supabase (sem ativar serviços pagos).
 */
interface CloudDatabaseAdapter {
    fun getStatus(configuredEndpoint: String): IntegrationAdapterStatus
    suspend fun prepareSyncPayload(positions: List<PortfolioItemView>): String
}

/**
 * Contrato preparado para futura sincronização de backups/estratégia com GitHub.
 */
interface GitSyncAdapter {
    fun getStatus(configuredRepo: String): IntegrationAdapterStatus
    suspend fun prepareStrategySnapshotMarkdown(summary: String): String
}

class SupabaseFutureAdapter : CloudDatabaseAdapter {
    override fun getStatus(configuredEndpoint: String): IntegrationAdapterStatus {
        return IntegrationAdapterStatus(
            providerName = "Supabase PostgreSQL (Futuro)",
            statusLabel = if (configuredEndpoint.isBlank()) "Desconectado • Armazenamento Local Ativo" else "Endpoint registrado (aguardando token seguro)",
            isActive = false,
            notes = "Arquitetura Repository desacoplada pronta para migrar DAO Room -> PostgREST/Supabase Auth sem custos nesta v1."
        )
    }

    override suspend fun prepareSyncPayload(positions: List<PortfolioItemView>): String {
        return "Payload JSON pronto (${positions.size} posições) — retenção exclusivamente local na v1."
    }
}

class GitHubFutureAdapter : GitSyncAdapter {
    override fun getStatus(configuredRepo: String): IntegrationAdapterStatus {
        return IntegrationAdapterStatus(
            providerName = "GitHub Sync (Futuro)",
            statusLabel = if (configuredRepo.isBlank()) "Preparado • Sem repositório vinculado" else "Repositório alvo: $configuredRepo",
            isActive = false,
            notes = "Pronto para versionar critérios de estratégia, relatórios CSV/MD e histórico de auditoria."
        )
    }

    override suspend fun prepareStrategySnapshotMarkdown(summary: String): String {
        return "# Snapshot Estratégia INVEST AI\n\n$summary"
    }
}

object ReportExporter {
    fun buildExecutiveReportText(
        realPositions: List<PortfolioItemView>,
        simPositions: List<PortfolioItemView>,
        realHhi: HhiRiskReport,
        simHhi: HhiRiskReport,
        latestImport: ImportHistoryEntity?,
        savedInsights: List<SavedAiRecommendationEntity> = emptyList()
    ): String {
        val fmtDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
        val consolidated = ConsolidatedReportAggregator.aggregate(
            savedInsights = savedInsights,
            realPositions = realPositions,
            realHhi = realHhi
        )
        val sb = StringBuilder()
        sb.appendLine("====================================================")
        sb.appendLine("INVEST AI — RELATÓRIO GERENCIAL CONSOLIDADO")
        sb.appendLine("Gerado em: $fmtDate")
        sb.appendLine("====================================================")
        sb.appendLine()
        sb.appendLine("1. CARTEIRA REAL (Armazenamento Local)")
        sb.appendLine("Referência de importação: ${latestImport?.fileName ?: "Nenhuma planilha importada"}")
        sb.appendLine("Total Investido: R$ ${"%.2f".format(realHhi.totalInvestedBrl)}")
        sb.appendLine("Saldo em Caixa:  R$ ${"%.2f".format(realHhi.cashBrl)}")
        sb.appendLine("Patrimônio Real: R$ ${"%.2f".format(realHhi.totalEquityBrl)}")
        sb.appendLine("Índice HHI Real: ${realHhi.hhiScore.toInt()} pts (${realHhi.classification})")
        sb.appendLine()
        if (realPositions.isEmpty()) {
            sb.appendLine("  * Nenhuma posição real registrada.")
        } else {
            sb.appendLine("  Ativo    | Classe      | Qtd     | PM (R$)  | Atual (R$) | Total (R$)  | Fonte")
            realPositions.forEach { p ->
                val cur = if (p.hasMarketPrice) "R$ ${"%.2f".format(p.currentPrice)}" else "Pendente"
                sb.appendLine("  ${p.ticker.padEnd(8)} | ${p.assetClass.shortLabel.padEnd(11)} | ${"%.2f".format(p.quantity).padEnd(7)} | ${"%.2f".format(p.averagePrice).padEnd(8)} | ${cur.padEnd(10)} | ${"%.2f".format(p.totalCurrentBrl).padEnd(11)} | ${p.dataSourceLabel}")
            }
        }
        sb.appendLine()
        sb.appendLine("2. ALERTAS DE CONCENTRAÇÃO E RISCO")
        realHhi.alerts.forEach { sb.appendLine("  - $it") }
        sb.appendLine()
        sb.appendLine("3. CARTEIRA DE SIMULAÇÃO (Separada da Real)")
        sb.appendLine("Patrimônio Simulado: R$ ${"%.2f".format(simHhi.totalEquityBrl)} (${simPositions.size} ativos | HHI: ${simHhi.hhiScore.toInt()} pts)")
        sb.appendLine()
        sb.appendLine("4. SÍNTESE CONSOLIDADA DOS AGENTES IA (ANALISTA • GESTOR • AUDITOR)")
        sb.appendLine("Cobertura dos Agentes: ${consolidated.coveredAgentsCount}/3 | Confiança Média: ${consolidated.averageConfidence}% | Consenso: ${consolidated.consensusConclusion?.label ?: "Pendente"}")
        listOf(
            AiAgentRole.ANALISTA to consolidated.analystInsight,
            AiAgentRole.GESTOR to consolidated.gestorInsight,
            AiAgentRole.AUDITOR to consolidated.auditorInsight
        ).forEach { (role, item) ->
            if (item != null) {
                sb.appendLine("  • ${role.title} [${item.ticker}]: ${item.conclusion} (${item.confidence}%) — ${item.thesis}")
                if (item.sourceLabel.isNotBlank()) sb.appendLine("    Fonte: ${item.sourceLabel} (Base: ${item.dataBase.ifBlank { "N/D" }})")
            } else {
                sb.appendLine("  • ${role.title}: Aguardando análise gerada.")
            }
        }
        sb.appendLine()
        sb.appendLine("Aviso: Plataforma exclusivamente analítica. Não executa ordens financeiras.")
        return sb.toString()
    }
}
