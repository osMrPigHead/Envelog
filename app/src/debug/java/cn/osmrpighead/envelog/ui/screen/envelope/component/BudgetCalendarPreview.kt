package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopePreviewData
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@LangPreview
@Composable
private fun BudgetCalendarPreview() = BudgetCalendarSample(EnvelopePreviewData.mixedCalendar)

@LangPreview
@Composable
private fun BudgetCalendarDailyPreview() = BudgetCalendarSample(EnvelopePreviewData.dailyCalendar)

@LangPreview
@Composable
private fun BudgetCalendarWorkdayPreview() = BudgetCalendarSample(EnvelopePreviewData.workdayCalendar)

@LangPreview
@Composable
private fun BudgetCalendarWeeklyPreview() = BudgetCalendarSample(EnvelopePreviewData.weeklyWorkdayCalendar)

@Composable
private fun BudgetCalendarSample(model: BudgetCalendarModel) {
    EnvelogTheme(dynamicColor = false) {
        BudgetCalendar(
            period = EnvelopePreviewData.period,
            model = model,
            firstDayOfWeek = EnvelopePreviewData.firstDayOfWeek,
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
        )
    }
}
