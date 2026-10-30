package cn.osmrpighead.envelog.ui.screen.overview

import cn.osmrpighead.envelog.data.entity.CurrencyType
import cn.osmrpighead.envelog.ui.screen.overview.model.EnvelopeModel
import cn.osmrpighead.envelog.ui.model.SummaryItem
import cn.osmrpighead.envelog.util.percent
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal object OverviewPreviewData {

    val yearMonth: YearMonth = YearMonth.of(2026, 10)
    val today: LocalDate = LocalDate.of(2026, 10, 19)

    val summaryItems: List<SummaryItem> = listOf(
        SummaryItem.Remaining(cents = 180_000, usedProportion = 320_000L.percent(500_000L)),
        SummaryItem.Expenses(cents = 320_000),
        SummaryItem.Balance(cents = -320_000),
    )

    val zeroSummaryItems: List<SummaryItem> = listOf(
        SummaryItem.Remaining(cents = 0, usedProportion = 0L.percent(0L)),
        SummaryItem.Expenses(cents = 0),
        SummaryItem.Balance(cents = 0),
    )

    val budgetedEnvelope: EnvelopeModel = EnvelopeModel(
        id = UUID.fromString("00000000-0000-0000-0000-000000000101"),
        name = "餐饮",
        currency = CurrencyType.CNY,
        usedAmount = 200_000,
        budgetRelated = EnvelopeModel.BudgetRelated(
            budgetAmount = 300_000,
            usedProportion = 200_000L.percent(300_000L),
            endDate = today.plusDays(10),
            today = today,
            currentAvailable = 40_000,
            diffAmount = 5_000,
        ),
    )

    val overspentEnvelope: EnvelopeModel = EnvelopeModel(
        id = UUID.fromString("00000000-0000-0000-0000-000000000102"),
        name = "交通",
        currency = CurrencyType.CNY,
        usedAmount = 120_000,
        budgetRelated = EnvelopeModel.BudgetRelated(
            budgetAmount = 200_000,
            usedProportion = 120_000L.percent(200_000L),
            endDate = null,
            today = today,
            currentAvailable = -20_000,
            diffAmount = -20_000,
        ),
    )

    val nonBudgetedEnvelope: EnvelopeModel = EnvelopeModel(
        id = UUID.fromString("00000000-0000-0000-0000-000000000103"),
        name = "购物",
        currency = CurrencyType.CNY,
        usedAmount = 30_000,
        budgetRelated = null,
    )

    val zeroBudgetEnvelope: EnvelopeModel = EnvelopeModel(
        id = UUID.fromString("00000000-0000-0000-0000-000000000104"),
        name = "娱乐",
        currency = CurrencyType.CNY,
        usedAmount = 0,
        budgetRelated = EnvelopeModel.BudgetRelated(
            budgetAmount = 0,
            usedProportion = 0L.percent(0L),
            endDate = null,
            today = today,
            currentAvailable = 0,
            diffAmount = 0,
        ),
    )

    val envelopes: List<EnvelopeModel> = listOf(
        budgetedEnvelope,
        overspentEnvelope,
        nonBudgetedEnvelope,
        zeroBudgetEnvelope,
    )

    val uiState: OverviewUiState = OverviewUiState(
        yearMonth = yearMonth,
        summaryItems = summaryItems,
        envelopes = envelopes,
    )

    val emptyUiState: OverviewUiState = OverviewUiState(
        yearMonth = yearMonth,
        summaryItems = emptyList(),
        envelopes = emptyList(),
    )
}
