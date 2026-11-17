package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.ImportHistoryEntity
import com.example.data.local.SavedAiRecommendationEntity
import com.example.domain.model.AiAgentRole
import com.example.domain.model.ConsolidatedReportAggregator
import com.example.domain.model.HhiRiskReport
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun `saveConsolidatedReport authenticated owner succeeds and streams via observeConsolidatedReports`() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreSyncRepository(firestore)

        val insights = listOf(
            SavedAiRecommendationEntity(
                agentRole = AiAgentRole.ANALISTA.name,
                ticker = "ITUB4",
                conclusion = "Comprar",
                confidence = 82,
                thesis = "ROE sólido e valuation atrativo.",
                usedPersonalData = false,
                sourceLabel = "RI Itaú / B3",
                dataBase = "12/11/2026"
            ),
            SavedAiRecommendationEntity(
                agentRole = AiAgentRole.GESTOR.name,
                ticker = "ITUB4",
                conclusion = "Manter",
                confidence = 78,
                thesis = "Peso equilibrado frente ao HHI.",
                usedPersonalData = true
            ),
            SavedAiRecommendationEntity(
                agentRole = AiAgentRole.AUDITOR.name,
                ticker = "ITUB4",
                conclusion = "Comprar",
                confidence = 88,
                thesis = "Múltiplos e proventos validados.",
                usedPersonalData = false
            )
        )
        val hhi = HhiRiskReport.calculate(emptyList(), cashBrl = 5000.0)
        val report = ConsolidatedReportAggregator.aggregate(insights, emptyList(), hhi, "ITUB4")
        val docId = "rep_${UUID.randomUUID().toString().replace("-", "_")}"

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveConsolidatedReport(report, hhi, docId = docId)
        }
        assertTrue("Expected saveConsolidatedReport to succeed: ${saveRes.exceptionOrNull()}", saveRes.isSuccess)

        val streamed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeConsolidatedReports(uid).first { list -> list.any { it.id == docId } }
        }
        val saved = streamed.first { it.id == docId }
        assertEquals("ITUB4", saved.tickerFilter)
        assertEquals("Comprar", saved.consensusConclusion)
        assertEquals(3L, saved.coveredAgentsCount)
        assertNotNull(saved.createdAt)
    }

    @Test
    fun `syncAgentInsight authenticated owner succeeds and cross-user read fails with permission denied`() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreSyncRepository(firestore)
        val entity = SavedAiRecommendationEntity(
            agentRole = AiAgentRole.AUDITOR.name,
            ticker = "HGLG11",
            conclusion = "Manter",
            confidence = 85,
            thesis = "Vacância controlada e fonte RI conferida.",
            usedPersonalData = false,
            sourceLabel = "CVM / RI",
            dataBase = "12/11/2026"
        )
        val docId = "ins_${UUID.randomUUID().toString().replace("-", "_")}"
        val res = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.syncAgentInsight(entity, docId) }
        assertTrue("Expected syncAgentInsight to succeed: ${res.exceptionOrNull()}", res.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.getAgentInsight(docId) }
        assertEquals("HGLG11", fetched.getOrNull()?.ticker)

        // Bob tries to list Alice's consolidated_reports directly
        val aliceUid = auth.currentUser!!.uid
        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreSyncRepository(firestore)
        var capturedError: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeConsolidatedReports(aliceUid).first()
            }
        } catch (t: Throwable) {
            capturedError = t
        }
        val firestoreEx = capturedError?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException for cross-user access, got $capturedError", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun `observeConsolidatedReports unauthenticated fails with permission denied`() = runBlocking {
        auth.signOut()
        val repo = FirestoreSyncRepository(firestore)
        var capturedError: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeConsolidatedReports("some_user_123").first()
            }
        } catch (t: Throwable) {
            capturedError = t
        }
        val firestoreEx = capturedError?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException for unauthenticated access, got $capturedError", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun `syncPortfolioSnapshot stores valid snapshot for authenticated owner`() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = FirestoreSyncRepository(firestore)
        val hhi = HhiRiskReport.calculate(emptyList(), cashBrl = 12000.0)
        val hist = ImportHistoryEntity(
            fileName = "carteira_b3.xlsx",
            fileType = "XLSX",
            recordCount = 6,
            cashBalanceBrl = 12000.0,
            isActiveRealReference = true,
            notes = "Importação limpa"
        )
        val snapId = "snap_${UUID.randomUUID().toString().replace("-", "_")}"
        val res = withTimeout(DEFAULT_TIMEOUT_MS) { repo.syncPortfolioSnapshot(hist, hhi, 6, snapId) }
        assertTrue("Expected syncPortfolioSnapshot to succeed: ${res.exceptionOrNull()}", res.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_invest@test.com"
        const val BOB_EMAIL = "bob_invest@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
