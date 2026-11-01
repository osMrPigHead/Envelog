package cn.osmrpighead.envelog.ui.screen.overview

import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.number.ratioOf
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.ui.screen.overview.model.SummaryItem
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Overview 预览数据。
 *
 * 信封直接使用查询模型 `EnvelopeSummary`，摘要项按 `OverviewViewModel` 的同一口径推导，
 * 这样主源根调整查询结果时，预览会随编译器一起失败而不是悄悄过期。
 */
internal object OverviewPreviewData {

    val yearMonth: YearMonth = YearMonth.of(2026, 10)
    val today: LocalDate = LocalDate.of(2026, 10, 19)

    private val budgetSpan = LocalDate.of(2026, 10, 1) untilExclusive LocalDate.of(2026, 10, 29)

    val summaryItems: List<SummaryItem> = listOf(
        SummaryItem.Remaining(cents = 150_000, usedProportion = 350_000L.ratioOf(500_000L)),
        SummaryItem.Expenses(cents = 350_000),
        SummaryItem.Balance(cents = -350_000),
    )

    val zeroSummaryItems: List<SummaryItem> = listOf(
        SummaryItem.Remaining(cents = 0, usedProportion = 0L.ratioOf(0L)),
        SummaryItem.Expenses(cents = 0),
        SummaryItem.Balance(cents = 0),
    )

    val budgetedEnvelope: EnvelopeSummary = EnvelopeSummary.Budgeted(
        id = EnvelopeId(UUID.fromString("00000000-0000-0000-0000-000000000101")),
        name = "餐饮",
        allocated = 200_000,
        currency = CurrencyType.CNY,
        archived = false,
        totalBudget = 300_000,
        currentBudget = 180_000,
        currentSpan = budgetSpan,
        currentSpanAllocated = 140_000,
        priorBudget = 150_000,
        priorAllocated = 145_000,
    )

    val overspentEnvelope: EnvelopeSummary = EnvelopeSummary.Budgeted(
        id = EnvelopeId(UUID.fromString("00000000-0000-0000-0000-000000000102")),
        name = "交通",
        allocated = 120_000,
        currency = CurrencyType.CNY,
        archived = false,
        totalBudget = 200_000,
        currentBudget = 100_000,
        currentSpan = budgetSpan,
        currentSpanAllocated = 120_000,
        priorBudget = 120_000,
        priorAllocated = 140_000,
    )

    val nonBudgetedEnvelope: EnvelopeSummary = EnvelopeSummary.NonBudget(
        id = EnvelopeId(UUID.fromString("00000000-0000-0000-0000-000000000103")),
        name = "购物",
        allocated = 30_000,
        currency = CurrencyType.CNY,
        archived = false,
    )

    val zeroBudgetEnvelope: EnvelopeSummary = EnvelopeSummary.Budgeted(
        id = EnvelopeId(UUID.fromString("00000000-0000-0000-0000-000000000104")),
        name = "娱乐",
        allocated = 0,
        currency = CurrencyType.CNY,
        archived = false,
        totalBudget = 0,
        currentBudget = 0,
        currentSpan = budgetSpan,
        currentSpanAllocated = 0,
        priorBudget = 0,
        priorAllocated = 0,
    )

    val envelopes: List<EnvelopeSummary> = listOf(
        budgetedEnvelope,
        overspentEnvelope,
        nonBudgetedEnvelope,
        zeroBudgetEnvelope,
    )

    val uiState: OverviewUiState = OverviewUiState.Success(
        yearMonth = yearMonth,
        today = today,
        summaryItems = summaryItems,
        envelopes = envelopes,
    )

    val emptyUiState: OverviewUiState = OverviewUiState.Success(
        yearMonth = yearMonth,
        today = today,
        summaryItems = emptyList(),
        envelopes = emptyList(),
    )
}
