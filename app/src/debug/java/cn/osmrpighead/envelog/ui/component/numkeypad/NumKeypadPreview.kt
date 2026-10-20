package cn.osmrpighead.envelog.ui.component.numkeypad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import java.math.RoundingMode

@Preview(showBackground = true)
@Composable
private fun NumKeypadPreview() {
    EnvelogTheme(dynamicColor = false) {
        var state by remember {
            mutableStateOf(
                NumKeypadState(
                    scale = 2,
                    roundingMode = RoundingMode.HALF_EVEN,
                    fatal = false,
                    leftOperand = "0",
                    rightOperand = "0",
                    operator = null,
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            Text(
                text = state.text,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
            NumKeypad(
                state = state,
                onStateChange = { state = it },
                confirmEnabled = true,
                onConfirm = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
