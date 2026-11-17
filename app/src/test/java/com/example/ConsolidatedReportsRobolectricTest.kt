package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.ui.navigation.InvestAppRoot
import com.example.ui.theme.InvestAiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConsolidatedReportsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `authenticated app root navigates across modules and renders consolidated reports dashboard`() {
        composeRule.setContent {
            InvestAiTheme {
                InvestAppRoot(userDisplayName = "Investidor Teste")
            }
        }

        composeRule.onNodeWithText("Dashboard Geral").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_carteira").performClick()
        composeRule.onNodeWithTag("import_spreadsheet_button").assertIsDisplayed()
        composeRule.onNodeWithTag("download_csv_template_button").assertExists()

        composeRule.onNodeWithTag("bottom_nav_analise_fundamentalista").performClick()
        composeRule.onNodeWithText("Análise Fundamentalista").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_simulador_carteira").performClick()
        composeRule.onNodeWithText("Simulador de Carteira").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_oportunidades_ia").performClick()
        composeRule.onNodeWithTag("ai_consent_switch").assertIsDisplayed()

        composeRule.onNodeWithTag("open_drawer_button").performClick()
        composeRule.onNodeWithTag("drawer_item_relatorios_gerenciais").performScrollTo().performClick()
        composeRule.onNodeWithText("Relatórios Gerenciais").assertIsDisplayed()
        composeRule.onNodeWithTag("run_consolidated_agents_button").assertIsDisplayed()
        composeRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("consolidated_consensus_card"))
        composeRule.onNodeWithTag("consolidated_consensus_card").assertExists()
        composeRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("agent_panel_analista"))
        composeRule.onNodeWithTag("agent_panel_analista").assertExists()
        composeRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("agent_panel_gestor"))
        composeRule.onNodeWithTag("agent_panel_gestor").assertExists()
        composeRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("agent_panel_auditor"))
        composeRule.onNodeWithTag("agent_panel_auditor").assertExists()
        composeRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("consolidated_risks_card"))
        composeRule.onNodeWithTag("consolidated_risks_card").assertExists()
        composeRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("sync_report_firestore_button"))
        composeRule.onNodeWithTag("sync_report_firestore_button").assertExists()
    }
}
