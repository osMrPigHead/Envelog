package cn.osmrpighead.envelog.ui.screen.overview.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.screen.overview.OverviewPreviewData
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@LangPreview
@Composable
private fun SummaryPreview() {
    EnvelogTheme(dynamicColor = false) {
        Summary(
            yearMonth = OverviewPreviewData.yearMonth,
            items = OverviewPreviewData.summaryItems,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacingBold))
                .padding(
                    top = dimensionResource(R.dimen.spacingBold),
                    bottom = dimensionResource(R.dimen.spacingMedium),
                ),
        )
    }
}

@LangPreview
@Composable
private fun SummaryWithoutBudgetPreview() {
    EnvelogTheme(dynamicColor = false) {
        Summary(
            yearMonth = OverviewPreviewData.yearMonth,
            items = OverviewPreviewData.zeroSummaryItems,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacingBold))
                .padding(
                    top = dimensionResource(R.dimen.spacingBold),
                    bottom = dimensionResource(R.dimen.spacingMedium),
                ),
        )
    }
}
