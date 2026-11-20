package cn.osmrpighead.envelog.ui.screen.overview.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenseAllocation
import cn.osmrpighead.envelog.data.model.ExpenseId
import cn.osmrpighead.envelog.data.model.ExpenseSubmission
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.ui.component.form.OptionalTextField
import cn.osmrpighead.envelog.ui.component.numkeypad.BottomAnimatedNumKeypad
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.lineHeightAsDp
import cn.osmrpighead.envelog.ui.util.hideAndThen
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordModalBottomSheet(
    formState: FormState<Unit>,
    selectedEnvelope: EnvelopeId,
    envelopes: List<EnvelopeSummary>,
    selectEnvelope: (EnvelopeId) -> Unit,
    submit: (ExpenseSubmission) -> Unit,
    close: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedEnvelope by rememberUpdatedState(selectedEnvelope)

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideAndClose: () -> Unit = { scope.launch { sheetState.hideAndThen(close) } }

    val imeVisible = WindowInsets.isImeVisible
    val numKeypadVisibility = remember { MutableTransitionState(!imeVisible) }
    numKeypadVisibility.targetState = !imeVisible

    val note = rememberTextFieldState()
    var numKeypadState by remember { mutableStateOf(NumKeypadState()) }
    val consumer = remember {
        AmountConsumer(
            scale = 2,
            roundingMode = RoundingMode.HALF_UP,
            submit = {
                submit(buildExpenseSubmission(selectedEnvelope, it, note.text.toString()))
            },
            cancel = hideAndClose,
        )
    }

    val listState = rememberLazyListState(envelopes.indexOfFirst { it.id == selectedEnvelope })

    LaunchedEffect(formState) {
        when (formState) {
            is FormState.Active -> numKeypadState = numKeypadState.copy(enabled = true)
            FormState.Busy -> numKeypadState = numKeypadState.copy(enabled = false)
            FormState.Completed -> hideAndClose()
        }
    }

    ModalBottomSheet(
        onDismissRequest = hideAndClose,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        modifier = modifier,
    ) {
        BackHandler { if (!imeVisible) hideAndClose() }

        Column(modifier = Modifier.wrapContentHeight()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space24)),
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space4)),
            ) {
                items(envelopes, { it.id.toString() }) {
                    FilterChip(
                        selected = selectedEnvelope == it.id,
                        onClick = { selectEnvelope(it.id) },
                        label = { Text(it.name) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.space48)))
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space16), Alignment.Bottom),
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.space24))
                    .padding(bottom = dimensionResource(R.dimen.space20)),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space4), Alignment.Bottom),
                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space12)),
                ) {
                    Text(
                        text = numKeypadState.text,
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    when (consumer.validationError) {
                        AmountValidationError.NonPositive -> ErrorText(stringResource(R.string.common_msg_amount_positive))
                        else -> Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeightAsDp))
                    }
                }
                OptionalTextField(
                    state = note,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(text = stringResource(R.string.record_expense_note_label)) }
            }

            BottomAnimatedNumKeypad(
                visibleState = numKeypadVisibility,
                state = numKeypadState,
                onStateChange = { numKeypadState = it },
                confirmEnabled = consumer.validate(numKeypadState.evaluate()),
                onConfirm = { consumer.onConfirm(numKeypadState.evaluate()) },
                onCancel = hideAndClose,
                modifier = Modifier.disableVerticalDrag(),
            )
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Right,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun buildExpenseSubmission(
    envelopeId: EnvelopeId,
    amount: Long,
    note: String,
): ExpenseSubmission {
    val id = ExpenseId.generate()
    return ExpenseSubmission(
        id = id,
        happenedAt = Instant.now(), // TODO: User select
        happenedZone = ZoneId.systemDefault(),
        allocations = listOf(
            ExpenseAllocation(
                expenseId = id,
                envelopeId = envelopeId,
                amount = amount,
                note = note,
            )
        )
    )
}
