package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.AuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE cardId = :cardId ORDER BY id DESC")
    fun getLogsForCard(cardId: String): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY id DESC")
    fun getLogsByAction(action: String): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE syncStatus = 'PENDING_SYNC' ORDER BY id ASC")
    suspend fun getPendingSyncLogs(): List<AuditLog>

    @Query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 1")
    suspend fun getLatestLog(): AuditLog?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: AuditLog): Long

    @Query("UPDATE audit_logs SET syncStatus = 'SYNCED' WHERE eventId IN (:eventIds)")
    suspend fun markLogsAsSynced(eventIds: List<String>)

    @Query("SELECT COUNT(*) FROM audit_logs WHERE syncStatus = 'PENDING_SYNC'")
    fun getPendingAuditCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM audit_logs")
    fun getTotalAuditCount(): Flow<Int>
}
