package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.layout.*
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
import cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield.OutlinedDayOfWeekRangeField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetField
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFieldError
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetFormState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.text.lineHeightAsDp

@Composable
internal fun QuickSetModeSection(
    enabled: Boolean,
    config: QuickSetFormState,
    fieldErrors: Set<QuickSetFieldError>,
    numKeypadHost: NumKeypadHostState,
    maxIntervalDays: Long,
    update: (QuickSetField) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dailySelected = config.mode == QuickSetMode.Daily
    val weeklySelected = config.mode == QuickSetMode.Weekly
    val monthlySelected = config.mode == QuickSetMode.Monthly

    val spacer: @Composable () -> Unit =
        { Spacer(Modifier.height(MaterialTheme.typography.bodySmall.lineHeightAsDp)) }

    Column(
        modifier = modifier.selectableGroup(),
    ) {
        QuickSetModeRow(
            dailySelected, enabled, fieldErrors, numKeypadHost, update, config.dailyConfig.amount,
            amountField = QuickSetField.Daily::Amount,
            amountDescription = stringResource(R.string.envelope_edit_quick_set_daily_amount_field),
            mode = QuickSetMode.Daily,
        ) {
            val intervalInvalid = QuickSetFieldError.IntervalInvalid in fieldErrors
            QuickSetIntervalField(
                days = config.dailyConfig.intervalDays,
                maxDays = maxIntervalDays,
                enabled = dailySelected && enabled,
                description = stringResource(R.string.envelope_edit_quick_set_interval_field),
                onValueChange = { days -> update(QuickSetField.Daily.Interval(days)) },
                modifier = Modifier.weight(1f),
                isError = dailySelected && intervalInvalid,
                supportingText = if (dailySelected && intervalInvalid) {
                    { Text(text = stringResource(R.string.envelope_edit_quick_set_interval_invalid)) }
                } else spacer,
            )
            it(false)
        }

        QuickSetModeRow(
            weeklySelected, enabled, fieldErrors, numKeypadHost, update, config.weeklyConfig.amount,
            amountField = QuickSetField.Weekly::Amount,
            amountDescription = stringResource(R.string.envelope_edit_quick_set_weekly_amount_field),
            mode = QuickSetMode.Weekly,
        ) {
            val weekdayDescription = stringResource(R.string.envelope_edit_quick_set_weekday_range_field)
            OutlinedDayOfWeekRangeField(
                startDayOfWeek = config.weeklyConfig.startDayOfWeek,
                endDayOfWeek = config.weeklyConfig.endDayOfWeek,
                onDayOfWeekRangeChange = { start, end -> update(QuickSetField.Weekly.DaySpan(start, end)) },
                enabled = weeklySelected && enabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                ),
//                colors = OutlinedTextFieldDefaults.colors(
//                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
//                ),
                supportingText = spacer,
                label = { Text(text = stringResource(R.string.envelope_edit_quick_set_mode_weekly_label)) },
                prefix = {
                    QuickSetAffix(
                        text = stringResource(R.string.envelope_edit_quick_set_interval_prefix),
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing2)),
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = weekdayDescription },
            )
            it(false)
        }

        QuickSetModeRow(
            monthlySelected, enabled, fieldErrors, numKeypadHost, update, config.monthlyConfig.amount,
            amountField = QuickSetField.Monthly::Amount,
            amountDescription = stringResource(R.string.envelope_edit_quick_set_monthly_amount_field),
            mode = QuickSetMode.Monthly,
        ) { it(true) }
    }
}

@Composable
private fun QuickSetModeRow(
    selected: Boolean,
    enabled: Boolean,
    fieldErrors: Set<QuickSetFieldError>,
    numKeypadHost: NumKeypadHostState,
    update: (QuickSetField) -> Unit,
    amount: Long,
    amountField: (Long) -> QuickSetField,
    amountDescription: String,
    mode: QuickSetMode,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(amountField: @Composable (Boolean) -> Unit) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = { update(QuickSetField.Mode(mode)) },
                role = Role.RadioButton,
            )
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing3)),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = MaterialTheme.typography.bodySmall.lineHeightAsDp),
            contentAlignment = Alignment.Center,
        ) {
            RadioButton(selected = selected, enabled = enabled, onClick = null)
        }
        content {
            QuickSetAmountField(
                amount = amount,
                fieldErrors = fieldErrors,
                host = numKeypadHost,
                update = { amount -> update(amountField(amount)) },
                selected = selected && enabled,
                description = amountDescription,
                modifier = Modifier.weight(1f),
                label = if (it) amountDescription else null,
            )
        }
    }
}
