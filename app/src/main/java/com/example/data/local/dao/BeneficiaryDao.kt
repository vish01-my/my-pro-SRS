package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.Beneficiary
import kotlinx.coroutines.flow.Flow

@Dao
interface BeneficiaryDao {

    @Query("SELECT * FROM beneficiaries WHERE cardId = :cardId LIMIT 1")
    suspend fun getBeneficiaryByCardId(cardId: String): Beneficiary?

    @Query("SELECT * FROM beneficiaries ORDER BY headOfFamilyName ASC")
    fun getAllBeneficiaries(): Flow<List<Beneficiary>>

    @Query("SELECT * FROM beneficiaries WHERE registeredFpsId = :fpsId")
    fun getBeneficiariesByFps(fpsId: String): Flow<List<Beneficiary>>

    @Query("SELECT COUNT(*) FROM beneficiaries WHERE cardStatus = 'ACTIVE'")
    fun getActiveBeneficiaryCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: Beneficiary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiaries(beneficiaries: List<Beneficiary>)

    @Update
    suspend fun updateBeneficiary(beneficiary: Beneficiary)

    @Query("UPDATE beneficiaries SET cardStatus = :status WHERE cardId = :cardId")
    suspend fun updateCardStatus(cardId: String, status: String)

    @Query("DELETE FROM beneficiaries WHERE cardId = :cardId")
    suspend fun deleteBeneficiary(cardId: String)
}
