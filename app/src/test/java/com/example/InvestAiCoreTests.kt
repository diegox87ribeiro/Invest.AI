package com.example

import com.example.data.importexport.SpreadsheetImporter
import com.example.data.local.SavedAiRecommendationEntity
import com.example.data.repository.MarketCatalogRepository
import com.example.data.repository.PlaceholderAiRepository
import com.example.data.repository.ReportExporter
import com.example.domain.model.AiAgentRole
import com.example.domain.model.AiConclusion
import com.example.domain.model.AssetClass
import com.example.domain.model.ConsolidatedReportAggregator
import com.example.domain.model.DataComGroup
import com.example.domain.model.HhiRiskReport
import com.example.domain.model.PortfolioItemView
import com.example.domain.model.PortfolioType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class InvestAiCoreTests {

    @Test
    fun `dataComGroup maps day ranges G1 to G4 accurately`() {
        assertEquals(DataComGroup.G1, DataComGroup.fromDay(1))
        assertEquals(DataComGroup.G1, DataComGroup.fromDay(8))
        assertEquals(DataComGroup.G2, DataComGroup.fromDay(9))
        assertEquals(DataComGroup.G2, DataComGroup.fromDay(16))
        assertEquals(DataComGroup.G3, DataComGroup.fromDay(17))
        assertEquals(DataComGroup.G3, DataComGroup.fromDay(24))
        assertEquals(DataComGroup.G4, DataComGroup.fromDay(25))
        assertEquals(DataComGroup.G4, DataComGroup.fromDay(31))
        assertEquals(null, DataComGroup.fromDay(null))
    }

    @Test
    fun `hhi calculates exact concentration score and alerts`() {
        val singleAsset = listOf(
            PortfolioItemView(
                id = 1L,
                portfolioType = PortfolioType.REAL,
                ticker = "ITUB4",
                name = "Itau",
                assetClass = AssetClass.ACOES_BR,
                quantity = 100.0,
                averagePrice = 30.0,
                currentPrice = 35.0,
                sector = "Bancos",
                dataComDay = 3,
                dataSourceLabel = "CSV",
                isDemo = false,
                hasMarketPrice = true
            )
        )
        val concentrated = HhiRiskReport.calculate(singleAsset, cashBrl = 0.0)
        assertEquals(10000.0, concentrated.hhiScore, 0.1)
        assertTrue(concentrated.classification.contains("Alta"))
        assertTrue(concentrated.alerts.any { it.contains("2.500") })

        val balanced = listOf(
            singleAsset.first(),
            singleAsset.first().copy(id = 2L, ticker = "HGLG11", assetClass = AssetClass.FII, sector = "Logística", quantity = 20.0, currentPrice = 175.0)
        )
        val moderateOrHigh = HhiRiskReport.calculate(balanced, cashBrl = 1000.0)
        assertEquals(5000.0, moderateOrHigh.hhiScore, 0.1)
    }

    @Test
    fun `csv importer parses brazilian decimals and never invents missing market prices`() {
        val csv = """
            Ticker;Classe;Quantidade;Preço Médio;Preço Atual;Setor;Data-Com
            HGLG11;FII;10;155,50;160,20;Logística;30
            KNCA11;FIAGRO;20;95,00;;Agro;30
            CAIXA;Renda Fixa;1;1200,00;;;
        """.trimIndent()

        val result = SpreadsheetImporter.parseCsv("minha_carteira.csv", csv)
        assertEquals(2, result.positions.size)
        assertEquals(1200.0, result.cashBalanceBrl ?: 0.0, 0.01)

        val hglg = result.positions[0]
        assertEquals("HGLG11", hglg.ticker)
        assertEquals(AssetClass.FII.name, hglg.assetClass)
        assertTrue(hglg.hasMarketPrice)
        assertEquals(160.20, hglg.currentPrice, 0.01)

        val knca = result.positions[1]
        assertEquals("KNCA11", knca.ticker)
        assertFalse(knca.hasMarketPrice)
        assertEquals(0.0, knca.currentPrice, 0.01)
        assertTrue(result.warnings.any { it.contains("KNCA11") })
    }

    @Test
    fun `xlsx importer parses native zip sharedStrings and sheet1 xml`() {
        val sharedStringsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="4" uniqueCount="4">
              <si><t>Ticker</t></si>
              <si><t>Quantidade</t></si>
              <si><t>Preco Medio</t></si>
              <si><t>BBAS3</t></si>
            </sst>
        """.trimIndent()

        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                  <c r="B1" t="s"><v>1</v></c>
                  <c r="C1" t="s"><v>2</v></c>
                </row>
                <row r="2">
                  <c r="A2" t="s"><v>3</v></c>
                  <c r="B2" t="n"><v>50</v></c>
                  <c r="C2" t="n"><v>26.40</v></c>
                </row>
              </sheetData>
            </worksheet>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        val result = SpreadsheetImporter.parseXlsx("carteira.xlsx", ByteArrayInputStream(baos.toByteArray()))
        assertEquals(1, result.positions.size)
        assertEquals("BBAS3", result.positions.first().ticker)
        assertEquals(50.0, result.positions.first().quantity, 0.01)
        assertEquals(26.40, result.positions.first().averagePrice, 0.01)
    }

    @Test
    fun `placeholder ai repository returns all required fields and respects privacy consent`() = runTest {
        val repo = PlaceholderAiRepository()
        val asset = MarketCatalogRepository.catalogAssets.first()
        val hhi = HhiRiskReport.calculate(emptyList(), 0.0)

        val rec = repo.generateRecommendation(
            role = AiAgentRole.AUDITOR,
            asset = asset,
            hhiReport = hhi,
            includePersonalPortfolio = false,
            customPrompt = ""
        )
        assertTrue(rec.isDemoPlaceholder)
        assertFalse(rec.usedPersonalPortfolioData)
        assertTrue(rec.tese.isNotBlank())
        assertTrue(rec.fonteConsultada.isNotBlank())
        assertTrue(rec.indicadoresRelevantes.isNotEmpty())
        assertTrue(rec.riscosIdentificados.isNotEmpty())
        assertTrue(rec.cenarioOtimista.isNotBlank())
        assertTrue(rec.cenarioBase.isNotBlank())
        assertTrue(rec.cenarioPessimista.isNotBlank())
        assertTrue(rec.nivelConfianca in 1..100)
    }

    @Test
    fun `csv importer decodes windows-1252 bytes and consolidates duplicate tickers with weighted average`() {
        val rawCsv = """
            Ticker;Classe;Quantidade;Preço Médio;Preço Atual;Setor;Data-Com
            ITUB4;Ações BR;100;30,00;;Bancos;3
            ITUB4;Ações BR;100;34,00;36,50;Bancos;
        """.trimIndent()
        val win1252Bytes = rawCsv.toByteArray(java.nio.charset.Charset.forName("windows-1252"))

        val result = SpreadsheetImporter.parseCsvBytes("corretora_ansi.csv", win1252Bytes)
        assertEquals(1, result.positions.size)
        val itub = result.positions.first()
        assertEquals("ITUB4", itub.ticker)
        assertEquals(AssetClass.ACOES_BR.name, itub.assetClass)
        assertEquals(200.0, itub.quantity, 0.01)
        assertEquals(32.0, itub.averagePrice, 0.01)
        assertTrue(itub.hasMarketPrice)
        assertEquals(36.50, itub.currentPrice, 0.01)
        assertEquals(3, itub.dataComDay)
        assertTrue(result.warnings.any { it.contains("consolidado") })
    }

    @Test
    fun `standard csv template round-trips cleanly through importer`() {
        val template = SpreadsheetImporter.generateStandardCsvTemplate()
        val parsed = SpreadsheetImporter.parseCsv("invest_ai_modelo_carteira.csv", template)

        assertEquals(5, parsed.positions.size)
        assertEquals(2500.0, parsed.cashBalanceBrl ?: 0.0, 0.01)
        assertTrue(parsed.positions.any { it.ticker == "ITUB4" && it.assetClass == AssetClass.ACOES_BR.name })
        assertTrue(parsed.positions.any { it.ticker == "HGLG11" && it.assetClass == AssetClass.FII.name })
        assertTrue(parsed.positions.any { it.ticker == "KNCA11" && it.assetClass == AssetClass.FIAGRO.name })
        assertTrue(parsed.positions.any { it.ticker == "IVVB11" && it.assetClass == AssetClass.ETF.name })
        assertTrue(parsed.positions.any { it.ticker == "SELIC29" && it.assetClass == AssetClass.RENDA_FIXA.name })

        val asViews = parsed.positions.mapIndexed { idx, e ->
            PortfolioItemView(
                id = idx.toLong() + 1,
                portfolioType = PortfolioType.REAL,
                ticker = e.ticker,
                name = e.name,
                assetClass = AssetClass.valueOf(e.assetClass),
                quantity = e.quantity,
                averagePrice = e.averagePrice,
                currentPrice = e.currentPrice,
                sector = e.sector,
                dataComDay = e.dataComDay,
                dataSourceLabel = e.dataSourceLabel,
                isDemo = false,
                hasMarketPrice = e.hasMarketPrice
            )
        }
        val exported = SpreadsheetImporter.exportPositionsToCsv(asViews, parsed.cashBalanceBrl ?: 0.0)
        val reimported = SpreadsheetImporter.parseCsv("export.csv", exported)
        assertEquals(5, reimported.positions.size)
        assertEquals(2500.0, reimported.cashBalanceBrl ?: 0.0, 0.01)
    }

    @Test
    fun `xlsx importer handles formula strings rich text shared strings and error cells without inventing prices`() {
        val sharedStringsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="4" uniqueCount="4">
              <si><t>Ativo</t></si>
              <si><t>Qtd</t></si>
              <si><t>Custo Médio</t></si>
              <si><t>Cotação</t></si>
            </sst>
        """.trimIndent()

        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                  <c r="B1" t="s"><v>1</v></c>
                  <c r="C1" t="s"><v>2</v></c>
                  <c r="D1" t="s"><v>3</v></c>
                </row>
                <row r="2">
                  <c r="A2" t="str"><v>XPML11</v></c>
                  <c r="B2" t="n"><v>30</v></c>
                  <c r="C2" t="n"><v>104.50</v></c>
                  <c r="D2" t="e"><v>#N/A</v></c>
                </row>
              </sheetData>
            </worksheet>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        val result = SpreadsheetImporter.parseXlsx("carteira_formula.xlsx", ByteArrayInputStream(baos.toByteArray()))
        assertEquals(1, result.positions.size)
        val xpml = result.positions.first()
        assertEquals("XPML11", xpml.ticker)
        assertEquals(AssetClass.FII.name, xpml.assetClass)
        assertFalse(xpml.hasMarketPrice)
        assertEquals(0.0, xpml.currentPrice, 0.01)
        assertTrue(result.warnings.any { it.contains("XPML11") })
    }

    @Test
    fun `consolidated report aggregator synthesizes all three agents consensus and risks`() {
        val insights = listOf(
            SavedAiRecommendationEntity(
                id = 1L,
                agentRole = AiAgentRole.ANALISTA.name,
                ticker = "HGLG11",
                conclusion = AiConclusion.COMPRAR.label,
                confidence = 80,
                thesis = "DY sólido e vacância baixa.",
                usedPersonalData = false,
                sourceLabel = "RI Pátria / B3",
                dataBase = "2026-11-12",
                risksSummary = "Abertura de juros longos",
                createdAt = 300L
            ),
            SavedAiRecommendationEntity(
                id = 2L,
                agentRole = AiAgentRole.GESTOR.name,
                ticker = "HGLG11",
                conclusion = AiConclusion.MANTER.label,
                confidence = 75,
                thesis = "Peso dentro do teto de 15%.",
                usedPersonalData = true,
                sourceLabel = "Motor Local HHI",
                dataBase = "2026-11-12",
                risksSummary = "Concentração em logística",
                createdAt = 200L
            ),
            SavedAiRecommendationEntity(
                id = 3L,
                agentRole = AiAgentRole.AUDITOR.name,
                ticker = "HGLG11",
                conclusion = AiConclusion.MANTER.label,
                confidence = 91,
                thesis = "Múltiplos conferidos contra informe mensal.",
                usedPersonalData = true,
                sourceLabel = "CVM / B3",
                dataBase = "2026-11-12",
                risksSummary = "Validar último informe",
                createdAt = 100L
            )
        )
        val missingPricePos = PortfolioItemView(
            id = 1L,
            portfolioType = PortfolioType.REAL,
            ticker = "KNCA11",
            name = "Kinea Agro",
            assetClass = AssetClass.FIAGRO,
            quantity = 10.0,
            averagePrice = 95.0,
            currentPrice = 0.0,
            sector = "Agro",
            dataComDay = 30,
            dataSourceLabel = "CSV",
            isDemo = false,
            hasMarketPrice = false
        )
        val hhi = HhiRiskReport.calculate(listOf(missingPricePos), cashBrl = 500.0)
        val report = ConsolidatedReportAggregator.aggregate(
            savedInsights = insights,
            realPositions = listOf(missingPricePos),
            realHhi = hhi,
            tickerFilter = "HGLG11"
        )

        assertEquals(3, report.coveredAgentsCount)
        assertEquals(82, report.averageConfidence)
        assertEquals(AiConclusion.MANTER, report.consensusConclusion)
        assertTrue(report.hasConsensusDivergence)
        assertEquals(1, report.missingPriceCount)
        assertTrue(report.combinedRisks.any { it.contains("Abertura de juros longos") })
        assertTrue(report.combinedRisks.any { it.contains("sem cotação") })

        val exportedTxt = ReportExporter.buildExecutiveReportText(
            realPositions = listOf(missingPricePos),
            simPositions = emptyList(),
            realHhi = hhi,
            simHhi = HhiRiskReport.calculate(emptyList(), 25000.0),
            latestImport = null,
            savedInsights = insights
        )
        assertTrue(exportedTxt.contains("SÍNTESE CONSOLIDADA DOS AGENTES IA"))
        assertTrue(exportedTxt.contains("Agente Analista [HGLG11]"))
        assertTrue(exportedTxt.contains("Agente Gestor [HGLG11]"))
        assertTrue(exportedTxt.contains("Agente Auditor [HGLG11]"))
    }
}

