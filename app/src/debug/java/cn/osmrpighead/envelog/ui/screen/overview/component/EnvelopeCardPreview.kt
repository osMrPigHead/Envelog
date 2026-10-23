package cn.osmrpighead.envelog.ui.screen.overview.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.screen.overview.OverviewPreviewData
import cn.osmrpighead.envelog.ui.model.EnvelopeModel
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@LangPreview
@Composable
private fun EnvelopeCardPreview() {
    EnvelogTheme(dynamicColor = false) {
        EnvelopeCardSample(model = OverviewPreviewData.budgetedEnvelope, managing = false)
    }
}

@LangPreview
@Composable
private fun EnvelopeCardOverspentPreview() {
    EnvelogTheme(dynamicColor = false) {
        EnvelopeCardSample(model = OverviewPreviewData.overspentEnvelope, managing = false)
    }
}

@LangPreview
@Composable
private fun EnvelopeCardNonBudgetPreview() {
    EnvelogTheme(dynamicColor = false) {
        EnvelopeCardSample(model = OverviewPreviewData.nonBudgetedEnvelope, managing = false)
    }
}

@LangPreview
@Composable
private fun EnvelopeCardManagingPreview() {
    EnvelogTheme(dynamicColor = false) {
        EnvelopeCardSample(model = OverviewPreviewData.budgetedEnvelope, managing = true)
    }
}

@Composable
private fun EnvelopeCardSample(
    model: EnvelopeModel,
    managing: Boolean,
) {
    EnvelopeCard(
        model = model,
        managing = managing,
        diffColor = { diffAmount ->
            if (diffAmount < 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        },
        onArchiveClick = {},
        onEditClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacingLarge),
                vertical = dimensionResource(R.dimen.spacingSmall),
            ),
    )
}
