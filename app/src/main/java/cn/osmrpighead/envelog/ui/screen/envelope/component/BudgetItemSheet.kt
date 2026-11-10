package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.ConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.screen.envelope.ActiveError
import cn.osmrpighead.envelog.ui.screen.envelope.ActiveError.*
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount

@Composable
fun BudgetItemSheet(
    budget: Budget,
    onSpanChange: (HalfOpenDateSpan) -> Unit,
    numKeypadHost: NumKeypadHostState,
    amountConsumer: AmountConsumer,
    fieldId: Any,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    error: ActiveError?,
    modifier: Modifier = Modifier,
) {
    LabeledBox(
        label = stringResource(R.string.budget_title),
        modifier = modifier,
    ) {
        DateRangePickerField(
            span = budget.dateSpan.toClosed(),
            dialogBuilder = DateRangePickerDialogBuilder.Default(
                ConfirmEventHandler.Default { onSpanChange(it.toHalfOpen()) },
            ),
            label = { Text(stringResource(R.string.budget_range)) },
            modifier = Modifier.fillMaxWidth(),
        )
        NumKeypadField(
            value = budget.amount.formatAsAmount(Formatter.AmountInteger),
            host = numKeypadHost,
            consumer = amountConsumer,
            fieldId = fieldId,
            isError = amountConsumer.validationError != null,
            supportingText = when (amountConsumer.validationError) {
                null -> null
                is AmountValidationError.MathError -> null
                AmountValidationError.NonPositive -> {
                    { Text(text = stringResource(R.string.amount_should_be_positive)) }
                }
            },
            label = { Text(stringResource(R.string.budget_amount)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacingMedium)),
        )
        if (error != null) Text(
            text = when (error) {
                EnvelopeArchived -> stringResource(R.string.envelope_archived)
                BudgetOverlaps -> stringResource(R.string.budget_overlaps)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacingMedium)),
        ) {
            TextButton(onClick = onDelete) {
                Text(text = stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    numKeypadHost.confirm()
                    onSave()
                },
                enabled = error == null,
            ) {
                Text(text = stringResource(R.string.ok))
            }
        }
    }
}
