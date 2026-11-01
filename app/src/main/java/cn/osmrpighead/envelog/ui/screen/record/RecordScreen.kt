package cn.osmrpighead.envelog.ui.screen.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount

@Composable
fun RecordScreen(
    uiState: RecordUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        bottomBar = { NumKeypadHost(uiState.numKeypadHost) },
        modifier = modifier
            .navigationBarsPadding(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            NumKeypadField(
                value = uiState.amount.formatAsAmount(Formatter.AmountStandard),
                host = uiState.numKeypadHost,
                consumer = uiState.numKeypadConsumer,
                fieldId = uiState.fieldId,
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.spacingBold))
                    .fillMaxWidth()
            )
        }
    }
}
