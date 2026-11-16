package cn.osmrpighead.envelog.ui.screen.record

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.lineHeightAsDp
import kotlinx.coroutines.flow.Flow
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    submit: (Long, String) -> Unit,
    back: () -> Unit,
    backEvent: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    var note by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableLongStateOf(0L) }
    val host = remember { NumKeypadHostState() }
    val fieldId = remember { Any() }
    val consumer = remember {
        AmountConsumer(
            scale = 2,
            roundingMode = RoundingMode.HALF_UP,
            submit = {
                amount = it
                submit(it, note)
            },
            cancel = back,
        )
    }

    LaunchedEffect(Unit) {
        host.request(fieldId, consumer)
        backEvent.collect { back() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.record_expense_title)) },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(
                            imageVector = Icon.ArrowBack,
                            contentDescription = stringResource(R.string.common_action_back),
                        )
                    }
                },
            )
        },
        bottomBar = { NumKeypadHost(host) },
        modifier = modifier.navigationBarsPadding(),
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing4), Alignment.Bottom),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(dimensionResource(R.dimen.spacing12)),
        ) {
            Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing6))) {
                Text(
                    text = if (host.isOpen) host.state.text else amount.formatAsAmount(Formatter.AmountStandard),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(R.dimen.spacing4)),
                )
                when (consumer.validationError) {
                    AmountValidationError.NonPositive -> ErrorText(text = stringResource(R.string.common_msg_amount_positive))
                    else -> Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeightAsDp))
                }
            }
            MaterialTheme(typography = MaterialTheme.typography.copy(bodyLarge = MaterialTheme.typography.bodyMedium, bodySmall = MaterialTheme.typography.bodySmall)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.record_expense_note_label)) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
