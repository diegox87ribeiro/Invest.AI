package com.example.domain.model

import kotlin.math.pow

enum class AssetClass(val label: String, val shortLabel: String) {
    ACOES_BR("Ações Brasileiras", "Ações BR"),
    FII("Fundos Imobiliários", "FIIs"),
    FIAGRO("FIAGROs", "FIAGRO"),
    ETF("ETFs", "ETFs"),
    RENDA_FIXA("Renda Fixa", "Renda Fixa"),
    CRIPTO("Criptomoedas", "Cripto")
}

enum class DataComGroup(
    val code: String,
    val label: String,
    val dayRange: IntRange
) {
    G1("G1", "G1 • Dias 1 a 8", 1..8),
    G2("G2", "G2 • Dias 9 a 16", 9..16),
    G3("G3", "G3 • Dias 17 a 24", 17..24),
    G4("G4", "G4 • Dias 25 a 31", 25..31);

    companion object {
        fun fromDay(day: Int?): DataComGroup? {
            if (day == null || day !in 1..31) return null
            return entries.firstOrNull { day in it.dayRange }
        }
    }
}

enum class PortfolioType(val label: String) {
    REAL("Carteira Real"),
    SIMULATION("Carteira de Simulação")
}

enum class OperationType(val label: String) {
    COMPRA("Compra"),
    VENDA("Venda"),
    PROVENTO("Provento"),
    APORTE_CAIXA("Aporte em Caixa"),
    RETIRADA_CAIXA("Retirada de Caixa")
}

enum class AiAgentRole(val title: String, val subtitle: String) {
    ANALISTA("Agente Analista", "Fundamentalista, Técnica & Dividendos"),
    GESTOR("Agente Gestor", "Alocação, Diversificação & Risco HHI"),
    AUDITOR("Agente Auditor", "Verificação de Dados, Cálculos & Fontes")
}

enum class AiConclusion(val label: String) {
    COMPRAR("Comprar"),
    MANTER("Manter"),
    VENDER("Vender"),
    AGUARDAR("Aguardar")
}

data class MarketAsset(
    val ticker: String,
    val name: String,
    val assetClass: AssetClass,
    val sector: String,
    val price: Double,
    val changePercent1d: Double,
    val dy12m: Double,
    val pvp: Double,
    val pl: Double,
    val roe: Double,
    val liquidezDiariaMilhoes: Double,
    val inadimplenciaPercent: Double,
    val vacanciaPercent: Double,
    val dividaLiquidaEbitda: Double,
    val dataComDay: Int?,
    val paymentDay: Int?,
    val lastDividendBrl: Double,
    val dividendConsistencyMonths: Int,
    val dividendHistory6m: List<Double>,
    val priceSeries30d: List<Double>,
    val rsi14: Double,
    val mm9: Double,
    val mm21: Double,
    val mm200: Double,
    val supportPrice: Double,
    val resistancePrice: Double,
    val indexer: String,
    val maturityDate: String?,
    val adminFeePercent: Double,
    val fgcCovered: Boolean,
    val sourceLabel: String,
    val dataBaseDate: String,
    val riskSummary: String,
    val isDemoData: Boolean = true
) {
    val dataComGroup: DataComGroup? get() = DataComGroup.fromDay(dataComDay)
}

data class AiRecommendation(
    val id: String,
    val agentRole: AiAgentRole,
    val ticker: String,
    val tese: String,
    val dadosUtilizados: String,
    val dataBase: String,
    val fonteConsultada: String,
    val indicadoresRelevantes: List<Pair<String, String>>,
    val riscosIdentificados: List<String>,
    val cenarioOtimista: String,
    val cenarioBase: String,
    val cenarioPessimista: String,
    val nivelConfianca: Int,
    val conclusao: AiConclusion,
    val usedPersonalPortfolioData: Boolean,
    val isDemoPlaceholder: Boolean = true
)

data class AllocationSlice(
    val label: String,
    val valueBrl: Double,
    val weightPercent: Double
)

data class HhiRiskReport(
    val totalInvestedBrl: Double,
    val cashBrl: Double,
    val totalEquityBrl: Double,
    val hhiScore: Double,
    val classification: String,
    val byAsset: List<AllocationSlice>,
    val byClass: List<AllocationSlice>,
    val bySector: List<AllocationSlice>,
    val alerts: List<String>
) {
    companion object {
        fun calculate(
            positions: List<PortfolioItemView>,
            cashBrl: Double
        ): HhiRiskReport {
            val totalInvested = positions.sumOf { it.totalCurrentBrl }.coerceAtLeast(0.0)
            val totalEquity = totalInvested + cashBrl.coerceAtLeast(0.0)
            if (totalInvested <= 0.0) {
                return HhiRiskReport(
                    totalInvestedBrl = 0.0,
                    cashBrl = cashBrl,
                    totalEquityBrl = totalEquity,
                    hhiScore = 0.0,
                    classification = "Sem posições alocadas",
                    byAsset = emptyList(),
                    byClass = emptyList(),
                    bySector = emptyList(),
                    alerts = listOf("Importe uma planilha XLSX/CSV ou registre posições para calcular a concentração HHI real.")
                )
            }

            val assetSlices = positions
                .groupBy { it.ticker }
                .map { (ticker, items) ->
                    val sum = items.sumOf { it.totalCurrentBrl }
                    val weight = (sum / totalInvested) * 100.0
                    AllocationSlice(ticker, sum, weight)
                }
                .sortedByDescending { it.weightPercent }

            val hhi = assetSlices.sumOf { it.weightPercent.pow(2.0) }
            val classification = when {
                hhi < 1500.0 -> "Baixa Concentração (Diversificada)"
                hhi <= 2500.0 -> "Concentração Moderada"
                else -> "Alta Concentração (Risco Elevado)"
            }

            val classSlices = positions
                .groupBy { it.assetClass.shortLabel }
                .map { (cls, items) ->
                    val sum = items.sumOf { it.totalCurrentBrl }
                    AllocationSlice(cls, sum, (sum / totalInvested) * 100.0)
                }
                .sortedByDescending { it.weightPercent }

            val sectorSlices = positions
                .groupBy { it.sector.ifBlank { "Não classificado" } }
                .map { (sec, items) ->
                    val sum = items.sumOf { it.totalCurrentBrl }
                    AllocationSlice(sec, sum, (sum / totalInvested) * 100.0)
                }
                .sortedByDescending { it.weightPercent }

            val alerts = buildList {
                if (hhi > 2500.0) {
                    add("HHI acima de 2.500 pontos (${hhi.toInt()}): carteira altamente concentrada em poucos ativos.")
                } else if (hhi >= 1500.0) {
                    add("HHI moderado (${hhi.toInt()}): avalie ampliar o número de emissores ou classes descorrelacionadas.")
                }
                val topAsset = assetSlices.firstOrNull()
                if (topAsset != null && topAsset.weightPercent > 20.0) {
                    add("Ativo ${topAsset.label} representa ${"%.1f".format(topAsset.weightPercent)}% da carteira investida (acima do teto prudencial de 20%).")
                }
                val topSector = sectorSlices.firstOrNull()
                if (topSector != null && topSector.weightPercent > 35.0) {
                    add("Setor '${topSector.label}' concentra ${"%.1f".format(topSector.weightPercent)}% do capital alocado.")
                }
                if (cashBrl <= 0.0) {
                    add("Caixa zerado: reserva de oportunidade não identificada para absorver correções ou datas-com.")
                }
                if (isEmpty()) {
                    add("Concentração dentro dos limites saudáveis de diversificação (HHI < 1.500 e nenhum ativo > 20%).")
                }
            }

            return HhiRiskReport(
                totalInvestedBrl = totalInvested,
                cashBrl = cashBrl,
                totalEquityBrl = totalEquity,
                hhiScore = hhi,
                classification = classification,
                byAsset = assetSlices,
                byClass = classSlices,
                bySector = sectorSlices,
                alerts = alerts
            )
        }
    }
}

data class PortfolioItemView(
    val id: Long,
    val portfolioType: PortfolioType,
    val ticker: String,
    val name: String,
    val assetClass: AssetClass,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double,
    val sector: String,
    val dataComDay: Int?,
    val dataSourceLabel: String,
    val isDemo: Boolean,
    val hasMarketPrice: Boolean
) {
    val totalCostBrl: Double get() = quantity * averagePrice
    val totalCurrentBrl: Double get() = quantity * (if (hasMarketPrice && currentPrice > 0) currentPrice else averagePrice)
    val profitLossBrl: Double get() = if (hasMarketPrice && currentPrice > 0) totalCurrentBrl - totalCostBrl else 0.0
    val profitLossPercent: Double
        get() = if (hasMarketPrice && totalCostBrl > 0) (profitLossBrl / totalCostBrl) * 100.0 else 0.0
}

data class BenchmarkSeries(
    val name: String,
    val currentLabel: String,
    val return12mPercent: Double,
    val normalizedPoints12m: List<Double>,
    val source: String
)

data class SimulationScenarioResult(
    val finalEquityBrl: Double,
    val totalContributedBrl: Double,
    val projectedDividendsMonthlyBrl: Double,
    val estimatedYieldAnnualPercent: Double,
    val stressedEquityPessimisticBrl: Double,
    val projectedCurveMonths: List<Double>
)
