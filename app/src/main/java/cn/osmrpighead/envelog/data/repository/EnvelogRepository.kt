package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.Expenditure
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface EnvelogRepository {
    fun observeOverview(today: LocalDate, yearMonth: YearMonth, dateSpan: HalfOpenDateSpan): Flow<List<EnvelopeSummary>>

    suspend fun archiveEnvelope(id: EnvelopeId): ArchiveResult

    suspend fun recordExpenditure(expenditure: Expenditure)
}