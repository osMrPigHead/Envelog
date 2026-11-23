package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.ui.component.ErrorSupportingText
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.FormConfirmActions
import cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield.OutlinedDateRangeField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetField
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormModalState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetPreview
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.style.formField
import cn.osmrpighead.envelog.ui.style.formSupportingText
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.withCurrency
import java.time.YearMonth

@Composable
fun QuickSetForm(
    yearMonth: YearMonth,
    currency: CurrencyType,
    modalState: QuickSetFormModalState,
    numKeypadHost: NumKeypadHostState,
    update: (QuickSetField) -> Unit,
    dismiss: () -> Unit,
    apply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = modalState.config

    Column(modifier = modifier) {
        LabeledBox(
            label = stringResource(R.string.envelope_edit_quick_set_title),
            modifier = Modifier.padding(dimensionResource(R.dimen.space12)),
        ) {
            TargetRangeField(
                yearMonth = yearMonth,
                span = config.targetSpan,
                enabled = config.mode != QuickSetMode.Monthly,
                onSpanChange = { update(QuickSetField.TargetSpan(it)) },
                modifier = Modifier.formField(),
            )
            QuickSetModeSection(
                currency = currency,
                enabled = modalState.state is FormState.Active && !modalState.dismissed,
                config = config,
                fieldErrors = modalState.fieldErrors,
                numKeypadHost = numKeypadHost,
                maxIntervalDays = config.targetSpan.length,
                update = update,
            )
            if (modalState.preview != null) QuickSetPreview(
                currency = currency,
                preview = modalState.preview,
                modifier = Modifier.formSupportingText(),
            )
            val error = (modalState.state as? FormState.Active)?.error
            if (error != null) ErrorSupportingText(
                text = when (error) {
                    QuickSetFormError.EnvelopeArchived -> stringResource(R.string.envelope_edit_msg_archived)
                    QuickSetFormError.CannotGenerate -> stringResource(R.string.envelope_edit_quick_set_cannot_generate)
                },
                modifier = Modifier.formSupportingText(),
            )
            FormConfirmActions(
                onCancel = dismiss,
                onConfirm = apply,
                confirmEnabled = !modalState.isError,
            )
        }
    }

    NumKeypadHost(
        numKeypadHost,
        modifier = Modifier.disableVerticalDrag(),
    )
}

@Composable
private fun QuickSetPreview(
    currency: CurrencyType,
    preview: QuickSetPreview,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier,
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.envelope_edit_quick_set_preview_span,
                count = preview.days,
                preview.days,
            ),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space4)))
        Text(
            text = preview.totalAmount.formatAsAmount(Formatter.AmountInteger).withCurrency(currency),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun TargetRangeField(
    yearMonth: YearMonth,
    span: ClosedDateSpan,
    enabled: Boolean,
    onSpanChange: (ClosedDateSpan) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectableDates = remember(yearMonth) {
        val monthSpan = yearMonth.toHalfOpenDateSpan()
        object : SelectableDates {
            override fun isSelectableYear(year: Int): Boolean = year == yearMonth.year
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis.toLocalDate() in monthSpan
        }
    }

    OutlinedDateRangeField(
        span = span,
        onSpanChange = onSpanChange,
        selectableDates = selectableDates,
        enabled = enabled,
        label = { Text(text = stringResource(R.string.envelope_edit_quick_set_range_label)) },
        modifier = modifier,
    )
}
