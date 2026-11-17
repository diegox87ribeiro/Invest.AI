package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ImportHistoryEntity
import com.example.data.local.SavedAiRecommendationEntity
import com.example.data.repository.CloudConsolidatedReport
import com.example.data.repository.FirestoreSyncRepository
import com.example.domain.model.ConsolidatedExecutiveReport
import com.example.domain.model.HhiRiskReport
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudReportsUiState {
    data object Loading : CloudReportsUiState
    data class Success(val reports: List<CloudConsolidatedReport>) : CloudReportsUiState
    data class Error(val message: String) : CloudReportsUiState
}

class CloudSyncViewModel(
    private val repository: FirestoreSyncRepository,
    private val currentUserId: String
) : ViewModel() {

    val cloudReports: StateFlow<CloudReportsUiState> = repository.observeConsolidatedReports(currentUserId)
        .map<List<CloudConsolidatedReport>, CloudReportsUiState> { CloudReportsUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(CloudReportsUiState.Error(rootError.message ?: "Falha ao sincronizar relatórios na nuvem"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CloudReportsUiState.Loading
        )

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun syncConsolidatedReportToCloud(
        report: ConsolidatedExecutiveReport,
        hhiReport: HhiRiskReport,
        latestImport: ImportHistoryEntity?,
        positionsCount: Int
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val insights = listOfNotNull(report.analystInsight, report.gestorInsight, report.auditorInsight)
            insights.forEach { entity: SavedAiRecommendationEntity ->
                repository.syncAgentInsight(entity)
            }
            val reportRes = repository.saveConsolidatedReport(report, hhiReport)
            repository.syncPortfolioSnapshot(latestImport, hhiReport, positionsCount)
            _isSyncing.value = false
            _syncMessage.value = reportRes.fold(
                onSuccess = { "Relatório consolidado e pareceres salvos no Firebase Firestore." },
                onFailure = { err -> "Erro ao salvar no Firestore: ${err.localizedMessage ?: "permissão/rede"}" }
            )
        }
    }
}
