package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.form.LabeledSwitch
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeField
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeForm

@Composable
fun EnvelopeForm(
    form: EnvelopeForm,
    dismissed: Boolean,
    update: (EnvelopeField) -> Unit,
) {
    val enabled = form.state is FormState.Active && !dismissed

    OutlinedTextField(
        value = form.envelope.name,
        enabled = enabled,
        onValueChange = { update(EnvelopeField.Name(it)) },
        label = { Text(text = stringResource(R.string.common_label_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    LabeledSwitch(
        checked = form.envelope.isBudgeted,
        enabled = enabled,
        onCheckedChange = { update(EnvelopeField.IsBudgeted(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacing4)),
    ) {
        Text(
            text = stringResource(R.string.envelope_edit_label_budgeted),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
