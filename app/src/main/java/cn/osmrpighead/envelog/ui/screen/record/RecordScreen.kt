package cn.osmrpighead.envelog.ui.screen.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordScreen(
    submit: (Long, String) -> Unit,
    back: () -> Unit,
    backEvent: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    val note = rememberTextFieldState()
    var amount by rememberSaveable { mutableLongStateOf(0L) }
    val host = remember { NumKeypadHostState() }
    val fieldId = remember { Any() }
    val consumer = remember {
        AmountConsumer(
            scale = 2,
            roundingMode = RoundingMode.HALF_UP,
            submit = {
                amount = it
                submit(it, note.text.toString())
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
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing8), Alignment.Bottom),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(dimensionResource(R.dimen.spacing12)),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing3), Alignment.Bottom),
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing6)),
            ) {
                Text(
                    text = if (host.isOpen) host.state.text else amount.formatAsAmount(Formatter.AmountConcise),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (consumer.validationError) {
                    AmountValidationError.NonPositive -> ErrorText(text = stringResource(R.string.common_msg_amount_positive))
                    else -> Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeightAsDp))
                }
            }
            TextField(
                state = note,
                placeholder = { Text(text = stringResource(R.string.record_expense_note_label), style = MaterialTheme.typography.bodyMedium) },
                textStyle = MaterialTheme.typography.bodyMedium,
                lineLimits = TextFieldLineLimits.SingleLine,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                ),
                contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                    top = 4.dp,
                    bottom = 4.dp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(48.dp),
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
