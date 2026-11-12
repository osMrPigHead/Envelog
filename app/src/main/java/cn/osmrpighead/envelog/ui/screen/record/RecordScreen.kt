package cn.osmrpighead.envelog.ui.screen.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    uiState: RecordUiState,
    back: () -> Unit,
    backEvent: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
        bottomBar = { NumKeypadHost(uiState.numKeypadHost) },
        modifier = modifier
            .navigationBarsPadding(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            NumKeypadField(
                value = uiState.amount.formatAsAmount(Formatter.AmountStandard),
                host = uiState.numKeypadHost,
                consumer = uiState.amountConsumer,
                fieldId = uiState.fieldId,
                isError = uiState.amountConsumer.validationError != null,
                supportingText = when (uiState.amountConsumer.validationError) {
                    null -> null
                    is AmountValidationError.MathError -> null
                    AmountValidationError.NonPositive -> {
                        { Text(text = stringResource(R.string.common_msg_amount_positive)) }
                    }
                },
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.spacingBold))
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
    }
}
