package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.importexport.ImportParseResult
import com.example.data.local.ImportHistoryEntity
import com.example.domain.model.AssetClass
import com.example.domain.model.ConsolidatedExecutiveReport
import com.example.domain.model.HhiRiskReport
import com.example.domain.model.PortfolioType
import com.example.ui.screens.AiAgentsScreen
import com.example.ui.screens.AssetClassExplorerScreen
import com.example.ui.screens.CryptoScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DividendCalendarScreen
import com.example.ui.screens.FundamentalAnalysisScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.ReportsAndArchitectureScreen
import com.example.ui.screens.RiskHhiScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.screens.TechnicalAnalysisScreen
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiAgentsViewModel
import com.example.ui.viewmodel.CloudReportsUiState
import com.example.ui.viewmodel.InvestDashboardState
import com.example.ui.viewmodel.InvestViewModel
import kotlinx.coroutines.launch

@Composable
fun InvestAppRoot(
    viewModel: InvestViewModel = viewModel(),
    aiAgentsViewModel: AiAgentsViewModel = viewModel(),
    userDisplayName: String = "Investidor",
    onSignOut: () -> Unit = {},
    cloudReportsUiState: CloudReportsUiState = CloudReportsUiState.Success(emptyList()),
    isCloudSyncing: Boolean = false,
    cloudSyncMessage: String? = null,
    onClearCloudSyncMessage: () -> Unit = {},
    onSyncConsolidatedReport: (ConsolidatedExecutiveReport, HhiRiskReport, ImportHistoryEntity?, Int) -> Unit = { _, _, _, _ -> }
) {
    val navController = rememberNavController()
    val state by viewModel.dashboardState.collectAsStateWithLifecycle()
    val selectedPortfolioType by viewModel.selectedPortfolioType.collectAsStateWithLifecycle()
    val importFeedback by viewModel.importFeedback.collectAsStateWithLifecycle()
    val pendingImportPreview by viewModel.pendingImportPreview.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(cloudSyncMessage) {
        cloudSyncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearCloudSyncMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: InvestRoutes.ROUTE_DASHBOARD
    val currentModule = ALL_INVEST_MODULES.firstOrNull { it.route == currentRoute } ?: ALL_INVEST_MODULES.first()

    val navigateTo: (String) -> Unit = { route ->
        if (route != currentRoute) {
            navController.navigate(route) {
                popUpTo(InvestRoutes.ROUTE_DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        val isMedium = maxWidth in 600.dp..839.dp

        if (isExpanded) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(272.dp), drawerContainerColor = DeepNavySurface) {
                        InvestSidebarContent(
                            currentRoute = currentRoute,
                            userDisplayName = userDisplayName,
                            onSelectModule = navigateTo,
                            onSignOut = onSignOut
                        )
                    }
                }
            ) {
                MainScaffoldBody(
                    currentModule = currentModule,
                    currentRoute = currentRoute,
                    showMenuButton = false,
                    showBottomBar = false,
                    showRail = false,
                    onOpenDrawer = {},
                    onNavigate = navigateTo,
                    snackbarHostState = snackbarHostState,
                    navController = navController,
                    viewModel = viewModel,
                    aiAgentsViewModel = aiAgentsViewModel,
                    state = state,
                    selectedPortfolioType = selectedPortfolioType,
                    importFeedback = importFeedback,
                    pendingImportPreview = pendingImportPreview,
                    cloudReportsUiState = cloudReportsUiState,
                    isCloudSyncing = isCloudSyncing,
                    onSyncConsolidatedReport = onSyncConsolidatedReport
                )
            }
        } else {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(292.dp), drawerContainerColor = DeepNavySurface) {
                        InvestSidebarContent(
                            currentRoute = currentRoute,
                            userDisplayName = userDisplayName,
                            onSelectModule = { route ->
                                scope.launch { drawerState.close() }
                                navigateTo(route)
                            },
                            onSignOut = {
                                scope.launch { drawerState.close() }
                                onSignOut()
                            }
                        )
                    }
                }
            ) {
                MainScaffoldBody(
                    currentModule = currentModule,
                    currentRoute = currentRoute,
                    showMenuButton = true,
                    showBottomBar = !isMedium,
                    showRail = isMedium,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigate = navigateTo,
                    snackbarHostState = snackbarHostState,
                    navController = navController,
                    viewModel = viewModel,
                    aiAgentsViewModel = aiAgentsViewModel,
                    state = state,
                    selectedPortfolioType = selectedPortfolioType,
                    importFeedback = importFeedback,
                    pendingImportPreview = pendingImportPreview,
                    cloudReportsUiState = cloudReportsUiState,
                    isCloudSyncing = isCloudSyncing,
                    onSyncConsolidatedReport = onSyncConsolidatedReport
                )
            }
        }
    }
}

@Composable
private fun InvestSidebarContent(
    currentRoute: String,
    userDisplayName: String,
    onSelectModule: (String) -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = "Logo INVEST AI", tint = EmeraldPrimary)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("INVEST AI", style = MaterialTheme.typography.titleLarge, color = EmeraldPrimary)
                Text(userDisplayName, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = SubtleBorder)
        Spacer(Modifier.height(8.dp))
        ModuleSection.entries.forEach { section ->
            Text(
                text = section.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            ALL_INVEST_MODULES.filter { it.section == section }.forEach { mod ->
                NavigationDrawerItem(
                    label = { Text("${mod.number}. ${mod.title}", style = MaterialTheme.typography.labelLarge) },
                    selected = currentRoute == mod.route,
                    onClick = { onSelectModule(mod.route) },
                    icon = { Icon(mod.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.padding(vertical = 2.dp).testTag("drawer_item_${mod.route}"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = EmeraldContainer,
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary
                    )
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        HorizontalDivider(color = SubtleBorder, modifier = Modifier.padding(vertical = 8.dp))
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Sair da Conta Google")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffoldBody(
    currentModule: InvestModuleDest,
    currentRoute: String,
    showMenuButton: Boolean,
    showBottomBar: Boolean,
    showRail: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigate: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    navController: NavHostController,
    viewModel: InvestViewModel,
    aiAgentsViewModel: AiAgentsViewModel,
    state: InvestDashboardState,
    selectedPortfolioType: PortfolioType,
    importFeedback: ImportParseResult?,
    pendingImportPreview: ImportParseResult?,
    cloudReportsUiState: CloudReportsUiState,
    isCloudSyncing: Boolean,
    onSyncConsolidatedReport: (ConsolidatedExecutiveReport, HhiRiskReport, ImportHistoryEntity?, Int) -> Unit
) {
    Scaffold(
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavySurface),
                navigationIcon = {
                    if (showMenuButton) {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("open_drawer_button")) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menu de módulos")
                        }
                    }
                },
                title = {
                    Column {
                        Text(currentModule.title, style = MaterialTheme.typography.titleMedium)
                        Text("INVEST AI • Módulo ${currentModule.number}/15", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                },
                actions = {
                    Surface(shape = RoundedCornerShape(8.dp), color = EmeraldContainer.copy(alpha = 0.6f), modifier = Modifier.padding(end = 12.dp)) {
                        Text("SOMENTE ANÁLISE", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = DeepNavySurface, windowInsets = WindowInsets.navigationBars) {
                    BOTTOM_QUICK_MODULES.forEach { mod ->
                        NavigationBarItem(
                            selected = currentRoute == mod.route,
                            onClick = { onNavigate(mod.route) },
                            icon = { Icon(mod.icon, contentDescription = mod.shortTitle) },
                            label = { Text(mod.shortTitle, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = EmeraldPrimary, selectedTextColor = EmeraldPrimary, indicatorColor = EmeraldContainer),
                            modifier = Modifier.testTag("bottom_nav_${mod.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (showRail) {
                NavigationRail(containerColor = DeepNavySurface) {
                    BOTTOM_QUICK_MODULES.forEach { mod ->
                        NavigationRailItem(
                            selected = currentRoute == mod.route,
                            onClick = { onNavigate(mod.route) },
                            icon = { Icon(mod.icon, contentDescription = mod.shortTitle) },
                            label = { Text(mod.shortTitle) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = EmeraldPrimary, selectedTextColor = EmeraldPrimary, indicatorColor = EmeraldContainer),
                            modifier = Modifier.testTag("rail_nav_${mod.route}")
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavHost(
                    navController = navController,
                    startDestination = InvestRoutes.ROUTE_DASHBOARD,
                    enterTransition = {
                        val dir = if (moduleIndexForRoute(targetState.destination.route) >= moduleIndexForRoute(initialState.destination.route)) {
                            AnimatedContentTransitionScope.SlideDirection.Start
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.End
                        }
                        fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                            slideIntoContainer(dir, tween(240, easing = FastOutSlowInEasing)) { it / 6 }
                    },
                    exitTransition = {
                        val dir = if (moduleIndexForRoute(targetState.destination.route) >= moduleIndexForRoute(initialState.destination.route)) {
                            AnimatedContentTransitionScope.SlideDirection.Start
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.End
                        }
                        fadeOut(tween(180)) +
                            slideOutOfContainer(dir, tween(220, easing = FastOutSlowInEasing)) { it / 10 }
                    },
                    popEnterTransition = {
                        fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(240, easing = FastOutSlowInEasing)) { it / 6 }
                    },
                    popExitTransition = {
                        fadeOut(tween(180)) +
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220, easing = FastOutSlowInEasing)) { it / 10 }
                    }
                ) {
                    composable(InvestRoutes.ROUTE_DASHBOARD) { DashboardScreen(state, onNavigate, viewModel::loadDemoRealPortfolio) }
                    composable(InvestRoutes.ROUTE_CARTEIRA) {
                        PortfolioScreen(
                            state = state, selectedType = selectedPortfolioType, importFeedback = importFeedback, pendingImportPreview = pendingImportPreview,
                            onSelectType = viewModel::selectPortfolioType, onImportUri = viewModel::previewSpreadsheetImport,
                            onConfirmPendingImport = viewModel::confirmPendingImport, onDismissPendingImport = viewModel::dismissPendingImport,
                            onDismissImportFeedback = viewModel::clearImportFeedback, onBuildTemplateCsv = viewModel::buildStandardCsvTemplate,
                            onBuildPortfolioCsv = viewModel::buildPortfolioCsvExport, onNotify = viewModel::notifyMessage,
                            onLoadDemoReal = viewModel::loadDemoRealPortfolio, onClearPortfolio = viewModel::clearPortfolio,
                            onAddManualPosition = viewModel::addManualPosition, onRecordOperation = viewModel::recordOperation,
                            onDeletePosition = viewModel::deletePosition, onUpdateCash = viewModel::updateCashBalance
                        )
                    }
                    composable(InvestRoutes.ROUTE_ACOES_BR) { AssetClassExplorerScreen(AssetClass.ACOES_BR, state.marketAssets) }
                    composable(InvestRoutes.ROUTE_FIIS) { AssetClassExplorerScreen(AssetClass.FII, state.marketAssets) }
                    composable(InvestRoutes.ROUTE_FIAGROS) { AssetClassExplorerScreen(AssetClass.FIAGRO, state.marketAssets) }
                    composable(InvestRoutes.ROUTE_ETFS) { AssetClassExplorerScreen(AssetClass.ETF, state.marketAssets) }
                    composable(InvestRoutes.ROUTE_RENDA_FIXA) { AssetClassExplorerScreen(AssetClass.RENDA_FIXA, state.marketAssets) }
                    composable(InvestRoutes.ROUTE_CRIPTO) { CryptoScreen(state, viewModel::updateCryptoAllocations, viewModel::resetCryptoWallet) }
                    composable(InvestRoutes.ROUTE_CALENDARIO) { DividendCalendarScreen(state) }
                    composable(InvestRoutes.ROUTE_FUNDAMENTALISTA) { FundamentalAnalysisScreen(state, viewModel::updateStrategyCriteria) }
                    composable(InvestRoutes.ROUTE_TECNICA) { TechnicalAnalysisScreen(state) }
                    composable(InvestRoutes.ROUTE_RISCO_HHI) { RiskHhiScreen(state) }
                    composable(InvestRoutes.ROUTE_SIMULADOR) { SimulatorScreen(state, viewModel::simulatePortfolioScenario) }
                    composable(InvestRoutes.ROUTE_OPORTUNIDADES_IA) {
                        AiAgentsScreen(state = state, onSetConsent = viewModel::setAiPortfolioConsent, aiAgentsViewModel = aiAgentsViewModel)
                    }
                    composable(InvestRoutes.ROUTE_RELATORIOS) {
                        ReportsAndArchitectureScreen(
                            state = state,
                            reportText = viewModel.buildReportText(),
                            onSaveFutureIntegrations = viewModel::setFutureIntegrations,
                            aiAgentsViewModel = aiAgentsViewModel,
                            cloudReportsUiState = cloudReportsUiState,
                            isCloudSyncing = isCloudSyncing,
                            onSyncConsolidatedReport = onSyncConsolidatedReport
                        )
                    }
                }
            }
        }
    }
}
