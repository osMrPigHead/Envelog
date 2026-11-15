package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFieldError
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetFormState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.text.lineHeightAsDp
import cn.osmrpighead.envelog.ui.util.nonPositiveToError
import java.math.RoundingMode
import java.time.DayOfWeek

/** 小键盘确认后的金额写入器：只接受 (config, amount) -> config 的纯变换，实例身份需跨重组稳定。 */
@Composable
private fun rememberAmountConsumer(
    config: QuickSetFormState,
    onUpdate: (QuickSetFormState) -> Unit,
    withAmount: (QuickSetFormState, Long) -> QuickSetFormState,
): AmountConsumer {
    val latestConfig by rememberUpdatedState(config)
    val latestOnUpdate by rememberUpdatedState(onUpdate)

    return remember {
        AmountConsumer(
            scale = 0,
            roundingMode = RoundingMode.HALF_UP,
            submit = { amount -> latestOnUpdate(withAmount(latestConfig, amount)) },
            cancel = {},
        )
    }
}

@Composable
internal fun QuickSetModeSection(
    config: QuickSetFormState,
    fieldErrors: Set<QuickSetFieldError>,
    numKeypadHost: NumKeypadHostState,
    onModeChange: () -> Unit,
    onTextInputFocus: () -> Unit,
    onPickerOpen: () -> Unit,
    maxIntervalDays: Int,
    onUpdate: (QuickSetFormState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dailySelected = config.mode == QuickSetMode.Daily
    val weeklySelected = config.mode == QuickSetMode.Weekly
    val monthlySelected = config.mode == QuickSetMode.MonthlyTotal
    val weekdayDescription = stringResource(R.string.envelope_edit_quick_set_weekday_range_field)

    val dailyFieldId = remember { Any() }
    val dailyConsumer = rememberAmountConsumer(config, onUpdate) { form, amount ->
        form.copy(dailyConfig = form.dailyConfig.copy(amount = amount))
    }
    val weeklyFieldId = remember { Any() }
    val weeklyConsumer = rememberAmountConsumer(config, onUpdate) { form, amount ->
        form.copy(weeklyConfig = form.weeklyConfig.copy(amount = amount))
    }
    val monthlyFieldId = remember { Any() }
    val monthlyConsumer = rememberAmountConsumer(config, onUpdate) { form, amount ->
        form.copy(monthlyConfig = form.monthlyConfig.copy(amount = amount))
    }

    val spacer: @Composable () -> Unit =
        { Spacer(Modifier.height(MaterialTheme.typography.bodySmall.lineHeightAsDp)) }
    val intervalSupporting: @Composable () -> Unit =
        { Text(text = stringResource(R.string.envelope_edit_quick_set_interval_invalid)) }
    val amountSupporting: @Composable () -> Unit =
        { Text(text = stringResource(R.string.common_msg_amount_positive)) }

    val intervalInvalid = QuickSetFieldError.IntervalInvalid in fieldErrors
    val amountNonPositive = QuickSetFieldError.AmountNonPositive in fieldErrors
    val (dailyNonPositive, dailyAmountError) = dailyConsumer.nonPositiveToError(
        amountNonPositive && dailySelected,
        dailyFieldId,
        numKeypadHost,
    )
    val (weeklyNonPositive, weeklyAmountError) = weeklyConsumer.nonPositiveToError(
        amountNonPositive && weeklySelected,
        weeklyFieldId,
        numKeypadHost,
    )
    val (monthlyNonPositive, monthlyAmountError) = monthlyConsumer.nonPositiveToError(
        amountNonPositive && monthlySelected,
        monthlyFieldId,
        numKeypadHost,
    )

    val selectMode: (QuickSetMode) -> Unit = { newMode ->
        if (config.mode != newMode) {
            onModeChange()
            onUpdate(config.copy(mode = newMode))
        }
    }
    val updateIntervalDays: (Int) -> Unit = { days ->
        onUpdate(config.copy(dailyConfig = config.dailyConfig.copy(intervalDays = days)))
    }
    val updateWeeklyRange: (DayOfWeek, DayOfWeek) -> Unit = { start, end ->
        onUpdate(
            config.copy(
                weeklyConfig = config.weeklyConfig.copy(
                    startDayOfWeek = start,
                    endDayOfWeek = end,
                ),
            ),
        )
    }

    Column(
        modifier = modifier.selectableGroup(),
    ) {
        QuickSetModeRow(
            selected = dailySelected,
            onSelect = { selectMode(QuickSetMode.Daily) },
        ) {
            QuickSetIntervalField(
                days = config.dailyConfig.intervalDays,
                maxDays = maxIntervalDays,
                enabled = dailySelected,
                description = stringResource(R.string.envelope_edit_quick_set_interval_field),
                onValueChange = updateIntervalDays,
                onFocus = onTextInputFocus,
                modifier = Modifier.weight(1f),
                isError = dailySelected && intervalInvalid,
                supportingText = if (dailySelected && intervalInvalid) intervalSupporting else spacer,
            )
            QuickSetAmountField(
                amount = config.dailyConfig.amount,
                host = numKeypadHost,
                consumer = dailyConsumer,
                fieldId = dailyFieldId,
                enabled = dailySelected,
                description = stringResource(R.string.envelope_edit_quick_set_daily_amount_field),
                modifier = Modifier.weight(1f),
                isError = dailyAmountError,
                supportingText = if (dailyNonPositive) amountSupporting else spacer,
            )
        }

        QuickSetModeRow(
            selected = weeklySelected,
            onSelect = { selectMode(QuickSetMode.Weekly) },
        ) {
            DayOfWeekRangePickerField(
                startDayOfWeek = config.weeklyConfig.startDayOfWeek,
                endDayOfWeek = config.weeklyConfig.endDayOfWeek,
                sheetBuilder = DayOfWeekRangePickerSheetBuilder.Default(
                    confirmEventHandler = DayOfWeekRangePickerConfirmEventHandler.Default(updateWeeklyRange),
                ),
                enabled = weeklySelected,
                textStyle = MaterialTheme.typography.bodyMedium,
                supportingText = spacer,
                label = { Text(text = stringResource(R.string.envelope_edit_quick_set_mode_weekly_label)) },
                onOpen = onPickerOpen,
                modifier = Modifier
                    .weight(1f)
                    .semantics { this.contentDescription = weekdayDescription },
            )
            QuickSetAmountField(
                amount = config.weeklyConfig.amount,
                host = numKeypadHost,
                consumer = weeklyConsumer,
                fieldId = weeklyFieldId,
                enabled = weeklySelected,
                description = stringResource(R.string.envelope_edit_quick_set_weekly_amount_field),
                modifier = Modifier.weight(1f),
                isError = weeklyAmountError,
                supportingText = if (weeklyNonPositive) amountSupporting else spacer,
            )
        }

        QuickSetModeRow(
            selected = monthlySelected,
            onSelect = { selectMode(QuickSetMode.MonthlyTotal) },
        ) {
            QuickSetAmountField(
                amount = config.monthlyConfig.amount,
                host = numKeypadHost,
                consumer = monthlyConsumer,
                fieldId = monthlyFieldId,
                enabled = monthlySelected,
                description = stringResource(R.string.envelope_edit_quick_set_monthly_amount_field),
                label = stringResource(R.string.envelope_edit_quick_set_monthly_amount_field),
                modifier = Modifier.weight(1f),
                isError = monthlyAmountError,
                supportingText = if (monthlyNonPositive) amountSupporting else spacer,
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
