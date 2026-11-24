package com.moneytracker.vault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startMs AND :endMs")
    suspend fun getTransactionsInWindow(startMs: Long, endMs: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionsIgnoreDuplicates(items: List<TransactionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionIgnoreDuplicate(item: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreTransaction(item: TransactionEntity): Long

    @Query("SELECT smsFingerprint FROM transactions WHERE fullSmsBody = ''")
    suspend fun getFingerprintsMissingFullSmsBody(): List<String>

    @Query("UPDATE transactions SET fullSmsBody = :fullBody WHERE smsFingerprint = :fingerprint AND fullSmsBody = ''")
    suspend fun backfillFullSmsBody(fingerprint: String, fullBody: String)

    @Query(
        """
        UPDATE transactions 
        SET merchantDisplayName = :displayName,
            category = :category,
            note = :note,
            isIncludedInSpend = :isIncludedInSpend,
            isSelfTransfer = :isSelfTransfer,
            confidenceScore = 1.0,
            matchReason = 'Customized by you',
            isUserModified = 1
        WHERE id = :transactionId
        """
    )
    suspend fun updateSingleTransactionFull(
        transactionId: Long,
        displayName: String,
        category: String,
        note: String,
        isIncludedInSpend: Boolean,
        isSelfTransfer: Boolean
    )

    @Query(
        """
        UPDATE transactions 
        SET merchantDisplayName = :displayName,
            category = :category,
            isSelfTransfer = :isSelfTransfer,
            confidenceScore = 1.0,
            matchReason = 'Learned from your preference',
            isUserModified = 1
        WHERE UPPER(merchantRawCode) = UPPER(:rawCode)
          AND id != :excludeId
        """
    )
    suspend fun updateAllOtherMatchingMerchantCode(
        rawCode: String,
        displayName: String,
        category: String,
        isSelfTransfer: Boolean,
        excludeId: Long
    )

    @Query(
        """
        UPDATE transactions
        SET isIncludedInSpend = :included
        WHERE id = :transactionId
        """
    )
    suspend fun setTransactionIncludedInSpend(
        transactionId: Long,
        included: Boolean
    )

    @Query("UPDATE transactions SET isUserModified = 0 WHERE id = :transactionId")
    suspend fun resetUserModifiedFlag(transactionId: Long)

    @Query(
        """
        UPDATE transactions
        SET isSelfTransfer = 1,
            category = 'SELF_TRANSFER',
            matchReason = :reason
        WHERE id IN (:ids)
          AND isUserModified = 0
          AND category != 'CREDIT_CARD_BILL'
        """
    )
    suspend fun markIdsAsSelfTransfer(ids: List<Long>, reason: String)

    @Query(
        """
        UPDATE transactions
        SET merchantDisplayName = :resolvedName,
            category = :resolvedCategory,
            confidenceScore = 0.92,
            matchReason = 'Cross-learned from matching SMS history'
        WHERE UPPER(merchantRawCode) = UPPER(:rawCode)
          AND isUserModified = 0
          AND category = 'OTHERS'
        """
    )
    suspend fun propagateCrossLearnedMerchant(
        rawCode: String,
        resolvedName: String,
        resolvedCategory: String
    )

    @Query("SELECT * FROM learned_merchants ORDER BY updatedAt DESC")
    fun observeLearnedMerchants(): Flow<List<LearnedMerchantEntity>>

    @Query("SELECT * FROM learned_merchants")
    suspend fun getAllLearnedMerchants(): List<LearnedMerchantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearnedMerchant(entity: LearnedMerchantEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<Long>)

    @Query("DELETE FROM transactions WHERE senderAddress LIKE 'DEMO-%'")
    suspend fun clearDemoTransactions()
}
