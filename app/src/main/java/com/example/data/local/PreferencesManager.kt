package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.investPrefsDataStore by preferencesDataStore(name = "invest_ai_prefs")

data class AppPreferencesState(
    val cashRealBrl: Double = 0.0,
    val cashSimBrl: Double = 25000.0,
    val aiPortfolioConsentGranted: Boolean = false,
    val supabaseEndpointConfigured: String = "",
    val githubRepoConfigured: String = ""
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val CASH_REAL = doublePreferencesKey("cash_real_brl")
        val CASH_SIM = doublePreferencesKey("cash_sim_brl")
        val AI_CONSENT = booleanPreferencesKey("ai_portfolio_consent")
        val SUPABASE_URL = stringPreferencesKey("supabase_endpoint")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.investPrefsDataStore.data.map { prefs ->
        AppPreferencesState(
            cashRealBrl = prefs[Keys.CASH_REAL] ?: 0.0,
            cashSimBrl = prefs[Keys.CASH_SIM] ?: 25000.0,
            aiPortfolioConsentGranted = prefs[Keys.AI_CONSENT] ?: false,
            supabaseEndpointConfigured = prefs[Keys.SUPABASE_URL] ?: "",
            githubRepoConfigured = prefs[Keys.GITHUB_REPO] ?: ""
        )
    }

    suspend fun setCashRealBrl(amount: Double) {
        context.investPrefsDataStore.edit { it[Keys.CASH_REAL] = amount.coerceAtLeast(0.0) }
    }

    suspend fun setCashSimBrl(amount: Double) {
        context.investPrefsDataStore.edit { it[Keys.CASH_SIM] = amount.coerceAtLeast(0.0) }
    }

    suspend fun setAiPortfolioConsent(granted: Boolean) {
        context.investPrefsDataStore.edit { it[Keys.AI_CONSENT] = granted }
    }

    suspend fun setFutureIntegrationMeta(supabaseUrl: String, githubRepo: String) {
        context.investPrefsDataStore.edit {
            it[Keys.SUPABASE_URL] = supabaseUrl.trim()
            it[Keys.GITHUB_REPO] = githubRepo.trim()
        }
    }
}
