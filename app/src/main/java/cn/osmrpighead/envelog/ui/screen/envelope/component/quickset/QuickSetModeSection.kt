package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.form.DayOfWeekRangePickerConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DayOfWeekRangePickerField
import cn.osmrpighead.envelog.ui.component.form.DayOfWeekRangePickerSheetBuilder
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode

@Composable
internal fun QuickSetModeSection(
    formState: QuickSetFormState,
    controller: QuickSetSheetController,
    modifier: Modifier = Modifier,
) {
    val dailySelected = formState.mode == QuickSetMode.Daily
    val weeklySelected = formState.mode == QuickSetMode.Weekly
    val monthlySelected = formState.mode == QuickSetMode.MonthlyTotal
    val weekdayDescription = stringResource(R.string.envelope_edit_quick_set_weekday_range_field)

    val selectMode: (QuickSetMode) -> Unit = { newMode ->
        if (formState.mode != newMode) {
            controller.onModeChange()
            formState.selectMode(newMode)
        }
    }

    Column(
        modifier = modifier.selectableGroup(),
    ) {
        QuickSetModeRow(
            selected = dailySelected,
            onSelect = { selectMode(QuickSetMode.Daily) },
        ) {
            QuickSetIntervalField(
                value = formState.intervalFieldValue,
                maxDays = formState.maxIntervalDays,
                enabled = dailySelected,
                description = stringResource(R.string.envelope_edit_quick_set_interval_field),
                onValueChange = formState::updateIntervalField,
                onFocus = controller::onTextInputFocus,
                modifier = Modifier.weight(1f),
            )
            QuickSetAmountField(
                amount = formState.dailyAmount,
                host = controller.numKeypadHost,
                consumer = formState.dailyAmountConsumer,
                fieldId = formState.dailyAmountFieldId,
                enabled = dailySelected,
                description = stringResource(R.string.envelope_edit_quick_set_daily_amount_field),
                modifier = Modifier.weight(1f),
            )
        }

        QuickSetModeRow(
            selected = weeklySelected,
            onSelect = { selectMode(QuickSetMode.Weekly) },
        ) {
            DayOfWeekRangePickerField(
                startDayOfWeek = formState.weeklyStartDayOfWeek,
                endDayOfWeek = formState.weeklyEndDayOfWeek,
                sheetBuilder = DayOfWeekRangePickerSheetBuilder.Default(
                    confirmEventHandler = DayOfWeekRangePickerConfirmEventHandler.Default(
                        formState::updateWeeklyRange,
                    ),
                ),
                enabled = weeklySelected,
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text(text = stringResource(R.string.envelope_edit_quick_set_mode_weekly_label)) },
                onOpen = controller::onPickerOpen,
                modifier = Modifier
                    .weight(1f)
                    .semantics { this.contentDescription = weekdayDescription },
            )
            QuickSetAmountField(
                amount = formState.weeklyAmount,
                host = controller.numKeypadHost,
                consumer = formState.weeklyAmountConsumer,
                fieldId = formState.weeklyAmountFieldId,
                enabled = weeklySelected,
                description = stringResource(R.string.envelope_edit_quick_set_weekly_amount_field),
                modifier = Modifier.weight(1f),
            )
        }

        QuickSetModeRow(
            selected = monthlySelected,
            onSelect = { selectMode(QuickSetMode.MonthlyTotal) },
        ) {
            QuickSetAmountField(
                amount = formState.monthlyAmount,
                host = controller.numKeypadHost,
                consumer = formState.monthlyAmountConsumer,
                fieldId = formState.monthlyAmountFieldId,
                enabled = monthlySelected,
                description = stringResource(R.string.envelope_edit_quick_set_monthly_amount_field),
                label = stringResource(R.string.envelope_edit_quick_set_monthly_amount_field),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickSetModeRow(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(vertical = dimensionResource(R.dimen.spacingMini)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingSmall)),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            RadioButton(selected = selected, onClick = null)
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingSmall)),
                verticalAlignment = Alignment.Bottom,
            ) {
                content()
            }
        },
    )
}
