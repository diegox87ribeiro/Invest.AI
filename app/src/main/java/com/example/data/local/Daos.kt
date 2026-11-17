package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_positions ORDER BY portfolioType ASC, ticker ASC")
    fun observeAllPositions(): Flow<List<PortfolioPositionEntity>>

    @Query("SELECT * FROM portfolio_operations ORDER BY createdAt DESC")
    fun observeAllOperations(): Flow<List<PortfolioOperationEntity>>

    @Query("SELECT * FROM import_history ORDER BY importedAt DESC")
    fun observeImportHistory(): Flow<List<ImportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPositions(items: List<PortfolioPositionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(item: PortfolioPositionEntity): Long

    @Query("DELETE FROM portfolio_positions WHERE id = :id")
    suspend fun deletePositionById(id: Long)

    @Query("DELETE FROM portfolio_positions WHERE portfolioType = :portfolioType")
    suspend fun clearPositionsByType(portfolioType: String)

    @Insert
    suspend fun insertOperation(op: PortfolioOperationEntity): Long

    @Query("DELETE FROM portfolio_operations WHERE portfolioType = :portfolioType")
    suspend fun clearOperationsByType(portfolioType: String)

    @Insert
    suspend fun insertImportHistory(history: ImportHistoryEntity): Long

    @Query("UPDATE import_history SET isActiveRealReference = 0")
    suspend fun clearActiveRealReference()

    @Transaction
    suspend fun replaceRealPortfolioFromImport(
        history: ImportHistoryEntity,
        positions: List<PortfolioPositionEntity>
    ): Long {
        clearActiveRealReference()
        val batchId = insertImportHistory(history.copy(isActiveRealReference = true))
        clearPositionsByType("REAL")
        insertPositions(positions.map { it.copy(importBatchId = batchId, portfolioType = "REAL") })
        return batchId
    }
}

@Dao
interface StrategyDao {
    @Query("SELECT * FROM strategy_criteria WHERE id = 1")
    fun observeCriteria(): Flow<StrategyCriteriaEntity?>

    @Query("SELECT * FROM strategy_change_log ORDER BY changedAt DESC")
    fun observeChangeLog(): Flow<List<StrategyChangeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCriteria(criteria: StrategyCriteriaEntity)

    @Insert
    suspend fun insertChangeLog(log: StrategyChangeLogEntity)
}

@Dao
interface CryptoVirtualDao {
    @Query("SELECT * FROM crypto_virtual_positions ORDER BY allocationBrl DESC")
    fun observePositions(): Flow<List<CryptoVirtualPositionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CryptoVirtualPositionEntity>)

    @Query("DELETE FROM crypto_virtual_positions")
    suspend fun clearAll()
}

@Dao
interface AiRecommendationDao {
    @Query("SELECT * FROM saved_ai_recommendations ORDER BY createdAt DESC LIMIT 30")
    fun observeSaved(): Flow<List<SavedAiRecommendationEntity>>

    @Insert
    suspend fun insert(item: SavedAiRecommendationEntity)
}
