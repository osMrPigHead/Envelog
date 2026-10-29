package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.Transaction
import cn.osmrpighead.envelog.data.model.TransactionType
import cn.osmrpighead.envelog.data.query.AllocationResult
import cn.osmrpighead.envelog.data.query.DateRange
import cn.osmrpighead.envelog.data.query.EnvelopeResult
import cn.osmrpighead.envelog.core.time.DatePeriod
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

interface EnvelogRepository {
    fun observeActiveEnvelopes(): Flow<List<Envelope>>
    fun observeActiveEnvelopesWithExtra(range: DateRange): Flow<List<EnvelopeResult>>
    fun observeBudgets(envelopeId: UUID, range: DateRange): Flow<List<Budget>>
    fun observeExpenditureEnvelopeAllocations(envelopeId: UUID, range: DateRange): Flow<List<AllocationResult>>

    fun observeTransactions(range: DateRange): Flow<List<Transaction>>
    fun observeTransactions(type: TransactionType, range: DateRange): Flow<List<Transaction>>

    suspend fun getEnvelope(envelopeId: UUID): Envelope
    suspend fun observeEnvelopeWithExtra(envelopeId: UUID, range: DateRange): Flow<EnvelopeResult>

    suspend fun createEnvelope(name: String, isBudgeted: Boolean): Result<UUID>
    suspend fun renameEnvelope(id: UUID, name: String): Result<Unit>
    suspend fun archiveEnvelope(id: UUID): Result<Unit>
    suspend fun deleteEnvelope(id: UUID): Result<Unit>

    suspend fun clearBudget(id: UUID): Result<Budget>
    suspend fun clearBudget(range: DateRange): Result<List<Budget>>
    suspend fun clearBudget(envelopeId: UUID, range: DateRange): Result<List<Budget>>
    suspend fun setBudget(envelopeId: UUID, period: DatePeriod, amount: Long): Result<UUID>
    suspend fun setBudget(id: UUID, amount: Long): Result<Unit>

    suspend fun recordExpenditure(
        dateTime: ZonedDateTime, amount: Long,
        allocations: Map<UUID, Long>,
    ): Result<UUID>

    suspend fun reallocate(transactionId: UUID, allocations: Map<UUID, Long>): Result<Unit>
    suspend fun reallocate(transactionId: UUID, amount: Long?, allocations: Map<UUID, Long>): Result<Unit>
    suspend fun deleteTransaction(id: UUID): Result<Unit>
}