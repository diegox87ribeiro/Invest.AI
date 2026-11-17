package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.importexport.ImportParseResult
import com.example.data.importexport.SpreadsheetImporter
import com.example.data.local.AppPreferencesState
import com.example.data.local.CryptoVirtualPositionEntity
import com.example.data.local.ImportHistoryEntity
import com.example.data.local.InvestDatabase
import com.example.data.local.PortfolioOperationEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.SavedAiRecommendationEntity
import com.example.data.local.StrategyChangeLogEntity
import com.example.data.local.StrategyCriteriaEntity
import com.example.data.repository.AiRepository
import com.example.data.repository.FirebaseAiRepository
import com.example.data.repository.GitHubFutureAdapter
import com.example.data.repository.IntegrationAdapterStatus
import com.example.data.repository.MarketCatalogRepository
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.ReportExporter
import com.example.data.repository.SupabaseFutureAdapter
import com.example.domain.model.AiAgentRole
import com.example.domain.model.AiRecommendation
import com.example.domain.model.AssetClass
import com.example.domain.model.BenchmarkSeries
import com.example.domain.model.HhiRiskReport
import com.example.domain.model.MarketAsset
import com.example.domain.model.OperationType
import com.example.domain.model.PortfolioItemView
import com.example.domain.model.PortfolioType
import com.example.domain.model.SimulationScenarioResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InvestDashboardState(
    val realPositions: List<PortfolioItemView> = emptyList(),
    val simPositions: List<PortfolioItemView> = emptyList(),
    val operations: List<PortfolioOperationEntity> = emptyList(),
    val importHistory: List<ImportHistoryEntity> = emptyList(),
    val latestRealImport: ImportHistoryEntity? = null,
    val preferences: AppPreferencesState = AppPreferencesState(),
    val realHhi: HhiRiskReport = HhiRiskReport.calculate(emptyList(), 0.0),
    val simHhi: HhiRiskReport = HhiRiskReport.calculate(emptyList(), 25000.0),
    val strategyCriteria: StrategyCriteriaEntity = StrategyCriteriaEntity(),
    val strategyChangeLog: List<StrategyChangeLogEntity> = emptyList(),
    val cryptoVirtualPositions: List<CryptoVirtualPositionEntity> = emptyList(),
    val savedAiRecommendations: List<SavedAiRecommendationEntity> = emptyList(),
    val marketAssets: List<MarketAsset> = MarketCatalogRepository.catalogAssets,
    val benchmarks: List<BenchmarkSeries> = MarketCatalogRepository.benchmarks12m,
    val supabaseStatus: IntegrationAdapterStatus = SupabaseFutureAdapter().getStatus(""),
    val githubStatus: IntegrationAdapterStatus = GitHubFutureAdapter().getStatus("")
)

class InvestViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InvestDatabase.getInstance(application)
    private val prefsManager = PreferencesManager(application)
    private val repository = PortfolioRepository(application, db, prefsManager)
    private val supabaseAdapter = SupabaseFutureAdapter()
    private val githubAdapter = GitHubFutureAdapter()

    private val _selectedPortfolioType = MutableStateFlow(PortfolioType.REAL)
    val selectedPortfolioType: StateFlow<PortfolioType> = _selectedPortfolioType.asStateFlow()

    private val _importFeedback = MutableStateFlow<ImportParseResult?>(null)
    val importFeedback: StateFlow<ImportParseResult?> = _importFeedback.asStateFlow()

    private val _pendingImportPreview = MutableStateFlow<ImportParseResult?>(null)
    val pendingImportPreview: StateFlow<ImportParseResult?> = _pendingImportPreview.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    val dashboardState: StateFlow<InvestDashboardState> = combine(
        repository.positionsFlow,
        repository.operationsFlow,
        repository.importHistoryFlow,
        repository.preferencesFlow,
        repository.strategyCriteriaFlow,
        repository.strategyChangeLogFlow,
        repository.cryptoVirtualPositionsFlow,
        repository.savedAiRecommendationsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val positions = values[0] as List<PortfolioItemView>
        @Suppress("UNCHECKED_CAST")
        val ops = values[1] as List<PortfolioOperationEntity>
        @Suppress("UNCHECKED_CAST")
        val imports = values[2] as List<ImportHistoryEntity>
        val prefs = values[3] as AppPreferencesState
        val criteria = values[4] as StrategyCriteriaEntity
        @Suppress("UNCHECKED_CAST")
        val strategyLog = values[5] as List<StrategyChangeLogEntity>
        @Suppress("UNCHECKED_CAST")
        val crypto = values[6] as List<CryptoVirtualPositionEntity>
        @Suppress("UNCHECKED_CAST")
        val savedAi = values[7] as List<SavedAiRecommendationEntity>

        val realPos = positions.filter { it.portfolioType == PortfolioType.REAL }
        val simPos = positions.filter { it.portfolioType == PortfolioType.SIMULATION }
        val latestReal = imports.firstOrNull { it.isActiveRealReference } ?: imports.firstOrNull()

        InvestDashboardState(
            realPositions = realPos,
            simPositions = simPos,
            operations = ops,
            importHistory = imports,
            latestRealImport = latestReal,
            preferences = prefs,
            realHhi = HhiRiskReport.calculate(realPos, prefs.cashRealBrl),
            simHhi = HhiRiskReport.calculate(simPos, prefs.cashSimBrl),
            strategyCriteria = criteria,
            strategyChangeLog = strategyLog,
            cryptoVirtualPositions = crypto,
            savedAiRecommendations = savedAi,
            marketAssets = MarketCatalogRepository.catalogAssets,
            benchmarks = MarketCatalogRepository.benchmarks12m,
            supabaseStatus = supabaseAdapter.getStatus(prefs.supabaseEndpointConfigured),
            githubStatus = githubAdapter.getStatus(prefs.githubRepoConfigured)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InvestDashboardState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialDefaults()
            repository.initDefaultCryptoWalletIfEmpty(dashboardState.value.cryptoVirtualPositions)
        }
    }

    fun selectPortfolioType(type: PortfolioType) {
        _selectedPortfolioType.value = type
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun clearImportFeedback() {
        _importFeedback.value = null
    }

    fun previewSpreadsheetImport(uri: Uri) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.parseSpreadsheetPreview(uri)
                }
                if (result.positions.isNotEmpty()) {
                    _pendingImportPreview.value = result
                } else {
                    _importFeedback.value = result
                    _statusBannerMessage.value = result.warnings.firstOrNull()
                        ?: "Nenhum ativo válido encontrado na planilha."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = "Erro ao ler planilha: ${e.localizedMessage ?: "formato inválido"}"
            }
        }
    }

    fun confirmPendingImport() {
        val pending = _pendingImportPreview.value ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.commitRealPortfolioImport(pending)
                }
                _pendingImportPreview.value = null
                _importFeedback.value = pending
                _statusBannerMessage.value =
                    "Importação '${pending.fileName}' (${pending.positions.size} ativos) definida como referência da Carteira Real."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = "Erro ao salvar importação: ${e.localizedMessage ?: "falha de gravação"}"
            }
        }
    }

    fun dismissPendingImport() {
        _pendingImportPreview.value = null
    }

    fun buildStandardCsvTemplate(): String = SpreadsheetImporter.generateStandardCsvTemplate()

    fun buildPortfolioCsvExport(type: PortfolioType): String {
        val st = dashboardState.value
        val positions = if (type == PortfolioType.REAL) st.realPositions else st.simPositions
        val cash = if (type == PortfolioType.REAL) st.preferences.cashRealBrl else st.preferences.cashSimBrl
        return SpreadsheetImporter.exportPositionsToCsv(positions, cash)
    }

    fun notifyMessage(message: String) {
        _statusBannerMessage.value = message
    }

    fun importSpreadsheet(uri: Uri) {
        previewSpreadsheetImport(uri)
    }

    fun loadDemoRealPortfolio() {
        viewModelScope.launch {
            repository.loadDemoRealPortfolio()
            _statusBannerMessage.value =
                "Carteira demonstrativa (DADOS FICTÍCIOS) carregada localmente na aba Carteira Real."
        }
    }

    fun clearPortfolio(type: PortfolioType) {
        viewModelScope.launch {
            repository.clearPortfolio(type)
            _statusBannerMessage.value = "${type.label} limpa localmente."
        }
    }

    fun addManualPosition(
        portfolioType: PortfolioType,
        ticker: String,
        name: String,
        assetClass: AssetClass,
        quantity: Double,
        averagePrice: Double,
        currentPrice: Double?,
        sector: String,
        dataComDay: Int?
    ) {
        viewModelScope.launch {
            repository.addManualPosition(
                portfolioType = portfolioType,
                ticker = ticker,
                name = name,
                assetClass = assetClass,
                quantity = quantity,
                averagePrice = averagePrice,
                currentPrice = currentPrice,
                sector = sector,
                dataComDay = dataComDay
            )
            _statusBannerMessage.value = "Posição ${ticker.uppercase()} registrada em ${portfolioType.label}."
        }
    }

    fun recordOperation(
        portfolioType: PortfolioType,
        ticker: String,
        operationType: OperationType,
        quantity: Double,
        unitPrice: Double,
        notes: String
    ) {
        viewModelScope.launch {
            repository.recordOperation(portfolioType, ticker, operationType, quantity, unitPrice, notes)
            _statusBannerMessage.value = "Operação ${operationType.label} registrada localmente."
        }
    }

    fun deletePosition(id: Long) {
        viewModelScope.launch { repository.deletePosition(id) }
    }

    fun updateCashBalance(type: PortfolioType, amount: Double) {
        viewModelScope.launch {
            repository.updateCashBalance(type, amount)
            _statusBannerMessage.value = "Saldo em caixa atualizado em ${type.label}."
        }
    }

    fun updateStrategyCriteria(newCriteria: StrategyCriteriaEntity, reason: String) {
        viewModelScope.launch {
            val current = dashboardState.value.strategyCriteria
            repository.updateStrategyCriteria(current, newCriteria, reason)
            _statusBannerMessage.value = "Critérios de estratégia atualizados e registrados no histórico."
        }
    }

    fun updateCryptoAllocations(btcPct: Double, ethPct: Double, solPct: Double) {
        viewModelScope.launch {
            repository.updateCryptoVirtualAllocations(btcPct, ethPct, solPct)
            _statusBannerMessage.value = "Carteira virtual cripto de R$ 500 rebalanceada (sem alavancagem/sem ordens reais)."
        }
    }

    fun resetCryptoWallet() {
        viewModelScope.launch {
            repository.resetCryptoVirtualWallet()
            _statusBannerMessage.value = "Carteira virtual de R$ 500 restaurada para alocação padrão."
        }
    }

    fun setAiPortfolioConsent(granted: Boolean) {
        viewModelScope.launch { repository.setAiConsent(granted) }
    }

    fun setFutureIntegrations(supabaseUrl: String, githubRepo: String) {
        viewModelScope.launch {
            repository.setFutureIntegrationMeta(supabaseUrl, githubRepo)
            _statusBannerMessage.value = "Configuração futura salva localmente (nenhum serviço pago ativado)."
        }
    }

    fun simulatePortfolioScenario(
        initialCapitalBrl: Double,
        monthlyContributionBrl: Double,
        horizonMonths: Int,
        annualReturnPercent: Double,
        annualDyPercent: Double,
        reinvestDividends: Boolean,
        macroShockPercent: Double
    ): SimulationScenarioResult {
        val monthlyRate = (annualReturnPercent / 100.0) / 12.0
        val monthlyDyRate = (annualDyPercent / 100.0) / 12.0
        var balance = initialCapitalBrl
        var contributed = initialCapitalBrl
        val curve = mutableListOf(balance)

        for (m in 1..horizonMonths.coerceIn(1, 240)) {
            val growth = balance * monthlyRate
            val dividends = balance * monthlyDyRate
            balance += growth + monthlyContributionBrl + if (reinvestDividends) dividends else 0.0
            contributed += monthlyContributionBrl
            curve.add(balance)
        }

        val finalDividendsMonthly = balance * monthlyDyRate
        val stressed = balance * (1.0 + (macroShockPercent / 100.0))

        return SimulationScenarioResult(
            finalEquityBrl = balance,
            totalContributedBrl = contributed,
            projectedDividendsMonthlyBrl = finalDividendsMonthly,
            estimatedYieldAnnualPercent = annualDyPercent,
            stressedEquityPessimisticBrl = stressed.coerceAtLeast(0.0),
            projectedCurveMonths = curve
        )
    }

    fun buildReportText(): String {
        val st = dashboardState.value
        return ReportExporter.buildExecutiveReportText(
            realPositions = st.realPositions,
            simPositions = st.simPositions,
            realHhi = st.realHhi,
            simHhi = st.simHhi,
            latestImport = st.latestRealImport,
            savedInsights = st.savedAiRecommendations
        )
    }
}
