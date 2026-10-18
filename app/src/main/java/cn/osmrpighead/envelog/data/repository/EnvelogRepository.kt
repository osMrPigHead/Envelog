package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction
import cn.osmrpighead.envelog.data.model.TransactionType
import cn.osmrpighead.envelog.data.query.AllocationResult
import cn.osmrpighead.envelog.data.query.EnvelopeResult
import cn.osmrpighead.envelog.data.query.ExpenditureResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

interface EnvelogRepository {
    fun observeActiveEnvelopes(): Flow<List<Envelope>>
    fun observeActiveEnvelopesWithExtra(startDate: LocalDate, endDate: LocalDate): Flow<List<EnvelopeResult>>
    fun observeBudgets(envelopeId: UUID, startDate: LocalDate, endDate: LocalDate): Flow<List<Budget>>
    fun observeExpenditureEnvelopeAllocations(
        envelopeId: UUID, startDate: LocalDate, endDate: LocalDate,
    ): Flow<List<AllocationResult>>
    fun observeTransactions(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>
    fun observeTransactions(type: TransactionType, startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>

    suspend fun createEnvelope(name: String, isBudgeted: Boolean): Result<UUID>
    suspend fun renameEnvelope(id: UUID, name: String): Result<Unit>
    suspend fun archiveEnvelope(id: UUID): Result<Unit>
    suspend fun deleteEnvelope(id: UUID): Result<Unit>

    suspend fun clearBudget(id: UUID): Result<Budget>
    suspend fun clearBudget(startDate: LocalDate, endDate: LocalDate): Result<List<Budget>>
    suspend fun clearBudget(envelopeId: UUID, startDate: LocalDate, endDate: LocalDate): Result<List<Budget>>
    suspend fun setBudget(envelopeId: UUID, startDate: LocalDate, endDate: LocalDate, amount: Long): Result<UUID>
    suspend fun setBudget(id: UUID, amount: Long): Result<Unit>

    suspend fun recordExpenditure(
        dateTime: ZonedDateTime, amount: Long,
        allocations: Map<UUID, Long>,
    ): Result<UUID>

    suspend fun reallocate(transactionId: UUID, allocations: Map<UUID, Long>): Result<Unit>
    suspend fun reallocate(transactionId: UUID, amount: Long?, allocations: Map<UUID, Long>): Result<Unit>
    suspend fun deleteTransaction(id: UUID): Result<Unit>
}