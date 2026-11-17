package com.example.data.repository

import com.example.domain.model.AiAgentRole
import com.example.domain.model.AiConclusion
import com.example.domain.model.AiRecommendation
import com.example.domain.model.HhiRiskReport
import com.example.domain.model.MarketAsset
import kotlinx.coroutines.delay
import java.util.UUID

interface AiRepository {
    val isLiveGeminiEnabled: Boolean
    suspend fun generateRecommendation(
        role: AiAgentRole,
        asset: MarketAsset,
        hhiReport: HhiRiskReport,
        includePersonalPortfolio: Boolean,
        customPrompt: String,
        horizonLabel: String = "Médio Prazo",
        focusLabel: String = "Análise Completa"
    ): AiRecommendation
}

/**
 * Implementação demonstrativa local utilizada quando o Firebase AI Logic não está ativo.
 * Nunca transmite planilhas pessoais nem armazena chaves de API no navegador/app.
 */
class PlaceholderAiRepository : AiRepository {
    override val isLiveGeminiEnabled: Boolean = false

    override suspend fun generateRecommendation(
        role: AiAgentRole,
        asset: MarketAsset,
        hhiReport: HhiRiskReport,
        includePersonalPortfolio: Boolean,
        customPrompt: String,
        horizonLabel: String,
        focusLabel: String
    ): AiRecommendation {
        delay(450) // Simula processamento analítico local sem bloquear a UI

        val portfolioContext = if (includePersonalPortfolio && hhiReport.totalInvestedBrl > 0) {
            val weight = hhiReport.byAsset.firstOrNull { it.label == asset.ticker }?.weightPercent ?: 0.0
            "Autorização explícita concedida: posição atual de ${"%.1f".format(weight)}% em ${asset.ticker}, HHI da carteira em ${hhiReport.hhiScore.toInt()} pts."
        } else {
            "Dados pessoais da carteira isolados localmente (não enviados à IA)."
        }

        val price = asset.price
        val dy = asset.dy12m
        val pvp = asset.pvp

        return when (role) {
            AiAgentRole.ANALISTA -> {
                val conclusion = when {
                    asset.inadimplenciaPercent > 3.5 -> AiConclusion.AGUARDAR
                    pvp in 0.80..1.05 && dy >= 8.0 -> AiConclusion.COMPRAR
                    asset.rsi14 > 65.0 -> AiConclusion.MANTER
                    else -> AiConclusion.AGUARDAR
                }
                AiRecommendation(
                    id = UUID.randomUUID().toString(),
                    agentRole = role,
                    ticker = asset.ticker,
                    tese = "[EXEMPLO DEMONSTRATIVO] ${asset.name} (${asset.ticker}) negocia a R$ ${"%.2f".format(price)} com DY de ${"%.2f".format(dy)}% e P/VP de ${"%.2f".format(pvp)}x. " +
                        "Tecnicamente, o ativo opera com IFR(14) em ${"%.1f".format(asset.rsi14)} frente às médias MM9 (R$ ${"%.2f".format(asset.mm9)}) e MM21 (R$ ${"%.2f".format(asset.mm21)}). " +
                        (if (customPrompt.isNotBlank()) "Foco solicitado: \"$customPrompt\"." else "Avaliação combinada de fluxo de proventos e margem de segurança."),
                    dadosUtilizados = "Preço de referência, DY 12m, P/VP, ROE/Vacância, médias móveis (MM9/MM21/MM200) e IFR(14). $portfolioContext",
                    dataBase = asset.dataBaseDate,
                    fonteConsultada = asset.sourceLabel,
                    indicadoresRelevantes = listOf(
                        "Cotação Base" to "R$ ${"%.2f".format(price)}",
                        "DY (12m)" to "${"%.2f".format(dy)}%",
                        "P/VP" to "${"%.2f".format(pvp)}x",
                        "Liquidez Diária" to "R$ ${"%.1f".format(asset.liquidezDiariaMilhoes)}M",
                        "IFR (14)" to "${"%.1f".format(asset.rsi14)}"
                    ),
                    riscosIdentificados = listOf(
                        asset.riskSummary,
                        if (asset.inadimplenciaPercent > 2.0) "Inadimplência de ${"%.1f".format(asset.inadimplenciaPercent)}% requer acompanhamento mensal." else "Sensibilidade à taxa Selic terminal e curva DI futuro."
                    ),
                    cenarioOtimista = "Fechamento da curva de juros e manutenção de proventos levam o ativo à resistência de R$ ${"%.2f".format(asset.resistancePrice * 1.06)}.",
                    cenarioBase = "Estabilidade operacional mantém cota/ação próxima à faixa de R$ ${"%.2f".format(price)} a R$ ${"%.2f".format(asset.resistancePrice)}.",
                    cenarioPessimista = "Abertura de juros longos ou deterioração de crédito pressionam o preço ao suporte em R$ ${"%.2f".format(asset.supportPrice)}.",
                    nivelConfianca = if (asset.inadimplenciaPercent > 3.5) 68 else 79,
                    conclusao = conclusion,
                    usedPersonalPortfolioData = includePersonalPortfolio,
                    isDemoPlaceholder = true
                )
            }

            AiAgentRole.GESTOR -> {
                val weight = if (includePersonalPortfolio) {
                    hhiReport.byAsset.firstOrNull { it.label == asset.ticker }?.weightPercent ?: 0.0
                } else 0.0
                val conclusion = when {
                    includePersonalPortfolio && weight > 18.0 -> AiConclusion.MANTER
                    includePersonalPortfolio && hhiReport.hhiScore > 2500.0 && weight > 12.0 -> AiConclusion.AGUARDAR
                    else -> AiConclusion.COMPRAR
                }
                AiRecommendation(
                    id = UUID.randomUUID().toString(),
                    agentRole = role,
                    ticker = asset.ticker,
                    tese = "[EXEMPLO DEMONSTRATIVO] Sob a ótica de alocação e controle de risco HHI, ${asset.ticker} pertence à classe ${asset.assetClass.label} (${asset.sector}). " +
                        "Com liquidez diária de R$ ${"%.1f".format(asset.liquidezDiariaMilhoes)}M e Data-Com ${asset.dataComGroup?.code ?: "N/A"}, o ativo contribui para escalonamento de fluxo de caixa. $portfolioContext",
                    dadosUtilizados = "Matriz de alocação por classe, peso por emissor, HHI (${hhiReport.hhiScore.toInt()} pts), caixa disponível e grupo Data-Com.",
                    dataBase = asset.dataBaseDate,
                    fonteConsultada = "${asset.sourceLabel} + Motor Local HHI",
                    indicadoresRelevantes = listOf(
                        "HHI Atual" to "${hhiReport.hhiScore.toInt()} pts",
                        "Peso na Carteira" to if (includePersonalPortfolio) "${"%.1f".format(weight)}%" else "Isolado",
                        "Grupo Data-Com" to (asset.dataComGroup?.label ?: "Sem distribuição mensal"),
                        "Caixa Disponível" to "R$ ${"%.2f".format(hhiReport.cashBrl)}"
                    ),
                    riscosIdentificados = listOf(
                        "Risco de concentração setorial caso o segmento '${asset.sector}' supere 25% do patrimônio.",
                        asset.riskSummary
                    ),
                    cenarioOtimista = "Aporte fracionado reduz HHI global abaixo de 1.500 pontos e equilibra recebimentos entre G1–G4.",
                    cenarioBase = "Manutenção de peso teto entre 8% e 12% por ativo preserva relação risco/retorno.",
                    cenarioPessimista = "Concentração excessiva acima de 20% amplia drawdown específico do emissor.",
                    nivelConfianca = 82,
                    conclusao = conclusion,
                    usedPersonalPortfolioData = includePersonalPortfolio,
                    isDemoPlaceholder = true
                )
            }

            AiAgentRole.AUDITOR -> {
                val hasWarnings = asset.inadimplenciaPercent > 3.0 || asset.liquidezDiariaMilhoes < 1.5
                AiRecommendation(
                    id = UUID.randomUUID().toString(),
                    agentRole = role,
                    ticker = asset.ticker,
                    tese = "[EXEMPLO DEMONSTRATIVO] Auditoria de consistência para ${asset.ticker}: os múltiplos e proventos exibidos provêm de base demonstrativa local. " +
                        "Antes de qualquer decisão real, valide o último Informe Mensal na CVM/B3 e o Release de Resultados no portal de RI oficial.",
                    dadosUtilizados = "Verificação cruzada de consistência DY vs último provento, liquidez mínima, inadimplência e origem da fonte.",
                    dataBase = asset.dataBaseDate,
                    fonteConsultada = "Checklist Auditoria CVM / B3 / ANBIMA / RI (Demo)",
                    indicadoresRelevantes = listOf(
                        "Status da Fonte" to "Demonstrativa (requer importação/API)",
                        "Consistência Proventos" to "${asset.dividendConsistencyMonths}/12 meses",
                        "Inadimplência Reportada" to "${"%.1f".format(asset.inadimplenciaPercent)}%",
                        "Taxa Adm." to "${"%.2f".format(asset.adminFeePercent)}%"
                    ),
                    riscosIdentificados = listOf(
                        "Dados exibidos são fictícios/demonstrativos — não refletem cotação em tempo real da B3.",
                        if (hasWarnings) "Alerta de auditoria: liquidez reduzida ou inadimplência acima do limite conservador." else "Nenhum desvio matemático detectado nos cálculos locais."
                    ),
                    cenarioOtimista = "Planilha XLSX/CSV oficial importada e conferida contra extratos B3 (Área do Investidor).",
                    cenarioBase = "Manter registro auditado localmente sem transmissão automática de dados sensíveis.",
                    cenarioPessimista = "Decisões baseadas em cotações desatualizadas podem distorcer P/VP e DY real.",
                    nivelConfianca = 91,
                    conclusao = if (hasWarnings) AiConclusion.AGUARDAR else AiConclusion.MANTER,
                    usedPersonalPortfolioData = includePersonalPortfolio,
                    isDemoPlaceholder = true
                )
            }
        }
    }
}
