package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.SpanMatchMode
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface EnvelogRepository {
    fun observeOverview(today: LocalDate, yearMonth: YearMonth, dateSpan: HalfOpenDateSpan): Flow<List<EnvelopeSummary>>
    fun observeAllocations(envelopeId: EnvelopeId, dateSpan: HalfOpenDateSpan): Flow<List<AllocationWithInfo>>
    fun observeBudgets(envelopeId: EnvelopeId, dateSpan: HalfOpenDateSpan, mode: SpanMatchMode): Flow<List<Budget>>
    fun observeBudgets(envelopeId: EnvelopeId, attributedMonth: YearMonth): Flow<List<Budget>>
    fun observeDailyAllocationOverview(
        envelopeId: EnvelopeId,
        dateSpan: HalfOpenDateSpan,
    ): Flow<List<DailyAllocationSummary>>

    suspend fun getEnvelopeById(id: EnvelopeId): Envelope
    suspend fun updateEnvelope(envelope: Envelope)
    suspend fun insertEnvelope(envelope: Envelope, budgets: List<Budget>)
    suspend fun archiveEnvelope(id: EnvelopeId): ArchiveResult

    suspend fun updateBudget(budget: Budget)
    suspend fun insertBudget(budget: Budget)

    suspend fun recordExpenditure(expenditure: ExpenditureSubmission)
}