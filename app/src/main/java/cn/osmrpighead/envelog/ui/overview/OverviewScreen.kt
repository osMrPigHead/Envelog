package cn.osmrpighead.envelog.ui.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.overview.component.EnvelopeCard
import cn.osmrpighead.envelog.ui.overview.component.PromptBar
import cn.osmrpighead.envelog.ui.overview.component.Summary
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import java.time.YearMonth
import java.util.UUID

@Composable
fun OverviewScreen(
    uiState: OverviewViewModel.UiState,
    onDeleteClick: (UUID) -> Unit,
    onArchiveClick: (UUID) -> Unit,
    onEditClick: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    var managing by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier) {
        Summary(
            yearMonth = uiState.yearMonth,
            items = uiState.summaryItems,
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.spacingBold))
                .padding(
                    top = dimensionResource(R.dimen.spacingBold),
                    bottom = dimensionResource(R.dimen.spacingMedium),
                ),
        )
        PromptBar(
            managing = managing,
            onManageButtonClick = { managing = !managing },
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacingBold)),
        )
        LazyColumn {
            items(uiState.envelopes, key = { it.id }) {
                EnvelopeCard(
                    model = it,
                    managing = managing,
                    diffColor = if (it.diffNeg) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    onDeleteClick = { onDeleteClick(it.id) },
                    onArchiveClick = { onArchiveClick(it.id) },
                    onEditClick = { onEditClick(it.id) },
                    modifier = Modifier
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacingLarge),
                            vertical = dimensionResource(R.dimen.spacingSmall),
                        ),
                )
            }
        }
    }
}

@Preview
@Composable
fun OverviewScreenPreview() {
    EnvelogTheme {
        OverviewScreen(
            uiState = OverviewViewModel.UiState(
                yearMonth = YearMonth.now(),
                summaryItems = listOf(
                    SummaryItem(
                        type = SummaryType.Remaining,
                        value = "11451411.14",
                        captionValue = "91",
                    ),
                    SummaryItem(
                        type = SummaryType.Expenses,
                        value = "9191.78",
                    ),
                    SummaryItem(
                        type = SummaryType.Balance,
                        value = "-1451.14",
                    ),
                ),
                envelopes = listOf(
                    EnvelopeModel(
                        id = UUID.randomUUID(),
                        name = "餐饮",
                        usedAmount = "1415.14",
                        currency = CurrencyType.CNY,
                        diffNeg = false,
                        budgetRelated = BudgetRelated(
                            usedPercent = "74",
                            totalAmount = "1919.8",
                            availableFor = BudgetScope.Today,
                            availableNow = "67",
                            availableDiff = "+16",
                        )
                    ),
                    EnvelopeModel(
                        id = UUID.randomUUID(),
                        name = "娱乐",
                        usedAmount = "1415.14",
                        currency = CurrencyType.CNY,
                        diffNeg = true,
                        budgetRelated = BudgetRelated(
                            usedPercent = "74",
                            totalAmount = "1919.8",
                            availableFor = BudgetScope.Today,
                            availableNow = "67",
                            availableDiff = "-16",
                        )
                    ),
                ),
            ),
            onDeleteClick = {},
            onArchiveClick = {},
            onEditClick = {},
        )
    }
}
