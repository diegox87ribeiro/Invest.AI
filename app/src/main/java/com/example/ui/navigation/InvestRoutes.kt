package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

object InvestRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_CARTEIRA = "carteira"
    const val ROUTE_ACOES_BR = "acoes_br"
    const val ROUTE_FIIS = "fiis"
    const val ROUTE_FIAGROS = "fiagros"
    const val ROUTE_ETFS = "etfs"
    const val ROUTE_RENDA_FIXA = "renda_fixa"
    const val ROUTE_CRIPTO = "criptomoedas"
    const val ROUTE_CALENDARIO = "calendario_dividendos"
    const val ROUTE_FUNDAMENTALISTA = "analise_fundamentalista"
    const val ROUTE_TECNICA = "analise_tecnica"
    const val ROUTE_RISCO_HHI = "gestao_riscos_hhi"
    const val ROUTE_SIMULADOR = "simulador_carteira"
    const val ROUTE_OPORTUNIDADES_IA = "oportunidades_ia"
    const val ROUTE_RELATORIOS = "relatorios_gerenciais"
}

enum class ModuleSection(val label: String) {
    VISAO_GERAL("Principal"),
    MERCADOS("Classes de Ativos"),
    INTELIGENCIA("Análise & Estratégia"),
    GESTAO("Risco, IA & Relatórios")
}

data class InvestModuleDest(
    val route: String,
    val number: Int,
    val title: String,
    val shortTitle: String,
    val icon: ImageVector,
    val section: ModuleSection
)

val ALL_INVEST_MODULES: List<InvestModuleDest> = listOf(
    InvestModuleDest(InvestRoutes.ROUTE_DASHBOARD, 1, "Dashboard Geral", "Dashboard", Icons.Default.Dashboard, ModuleSection.VISAO_GERAL),
    InvestModuleDest(InvestRoutes.ROUTE_CARTEIRA, 2, "Carteira de Investimentos", "Carteira", Icons.Default.AccountBalanceWallet, ModuleSection.VISAO_GERAL),

    InvestModuleDest(InvestRoutes.ROUTE_ACOES_BR, 3, "Ações Brasileiras", "Ações BR", Icons.Default.ShowChart, ModuleSection.MERCADOS),
    InvestModuleDest(InvestRoutes.ROUTE_FIIS, 4, "Fundos Imobiliários", "FIIs", Icons.Default.Apartment, ModuleSection.MERCADOS),
    InvestModuleDest(InvestRoutes.ROUTE_FIAGROS, 5, "FIAGROs", "FIAGROs", Icons.Default.Agriculture, ModuleSection.MERCADOS),
    InvestModuleDest(InvestRoutes.ROUTE_ETFS, 6, "ETFs", "ETFs", Icons.Default.PieChart, ModuleSection.MERCADOS),
    InvestModuleDest(InvestRoutes.ROUTE_RENDA_FIXA, 7, "Renda Fixa", "Renda Fixa", Icons.Default.AccountBalance, ModuleSection.MERCADOS),
    InvestModuleDest(InvestRoutes.ROUTE_CRIPTO, 8, "Criptomoedas (R$ 500)", "Cripto", Icons.Default.CurrencyBitcoin, ModuleSection.MERCADOS),

    InvestModuleDest(InvestRoutes.ROUTE_CALENDARIO, 9, "Calendário Datas-Com", "Calendário", Icons.Default.CalendarMonth, ModuleSection.INTELIGENCIA),
    InvestModuleDest(InvestRoutes.ROUTE_FUNDAMENTALISTA, 10, "Análise Fundamentalista", "Fundam.", Icons.Default.QueryStats, ModuleSection.INTELIGENCIA),
    InvestModuleDest(InvestRoutes.ROUTE_TECNICA, 11, "Análise Técnica", "Técnica", Icons.Default.CandlestickChart, ModuleSection.INTELIGENCIA),

    InvestModuleDest(InvestRoutes.ROUTE_RISCO_HHI, 12, "Riscos & HHI", "Risco HHI", Icons.Default.Shield, ModuleSection.GESTAO),
    InvestModuleDest(InvestRoutes.ROUTE_SIMULADOR, 13, "Simulador de Carteira", "Simulador", Icons.Default.Science, ModuleSection.GESTAO),
    InvestModuleDest(InvestRoutes.ROUTE_OPORTUNIDADES_IA, 14, "Agentes IA & Sinais", "Agentes IA", Icons.Default.AutoAwesome, ModuleSection.GESTAO),
    InvestModuleDest(InvestRoutes.ROUTE_RELATORIOS, 15, "Relatórios Gerenciais", "Relatórios", Icons.Default.Assessment, ModuleSection.GESTAO)
)

val PRIMARY_NAVIGATION_ROUTES: List<String> = listOf(
    InvestRoutes.ROUTE_DASHBOARD,
    InvestRoutes.ROUTE_CARTEIRA,
    InvestRoutes.ROUTE_FUNDAMENTALISTA,
    InvestRoutes.ROUTE_SIMULADOR
)

val BOTTOM_QUICK_MODULES: List<InvestModuleDest> = listOf(
    ALL_INVEST_MODULES[0], // 1. Dashboard
    ALL_INVEST_MODULES[1], // 2. Carteira
    ALL_INVEST_MODULES[9], // 10. Análise Fundamentalista
    ALL_INVEST_MODULES[12], // 13. Simulador
    ALL_INVEST_MODULES[13] // 14. Agentes IA
)

fun moduleIndexForRoute(route: String?): Int {
    if (route == null) return 0
    val idx = ALL_INVEST_MODULES.indexOfFirst { it.route == route }
    return if (idx >= 0) idx else 0
}
