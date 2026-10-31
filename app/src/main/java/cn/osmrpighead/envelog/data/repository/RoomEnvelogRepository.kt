package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.db.dao.EnvelopeDao
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.data.query.EnvelopeSummaryRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

private fun EnvelopeSummaryRow.toDomain() = if (isBudgeted) {
    requireNotNull(totalBudget)
    requireNotNull(currentBudget)
    requireNotNull(currentSpanStartDate)
    requireNotNull(currentSpanEndDate)
    EnvelopeSummary.Budgeted(
        id, name, allocated, totalBudget, currentBudget,
        currentSpanStartDate untilExclusive currentSpanEndDate,
        currentSpanAllocated,
    )
} else EnvelopeSummary.NonBudget(id, name, allocated)

class RoomEnvelogRepository @Inject constructor(
    private val envelopeDao: EnvelopeDao,
) : EnvelogRepository {
    override fun observeOverview(today: LocalDate, dateSpan: HalfOpenDateSpan): Flow<List<EnvelopeSummary>> =
        envelopeDao.observeEnvelopeSummariesOverlaps(today, dateSpan.startDate, dateSpan.endDate)
            .map { rows -> rows.map { it.toDomain() } }
}