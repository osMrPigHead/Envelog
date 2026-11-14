package cn.osmrpighead.envelog.ui.screen.envelope2.component

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
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.ui.component.form.LabeledSwitch
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope2.EnvelopeForm

@Composable
fun EnvelopeForm(
    form: EnvelopeForm,
    update: (Envelope) -> Unit,
) {
    OutlinedTextField(
        value = form.envelope.name,
        enabled = form.state is FormState.Active,
        onValueChange = { update(form.envelope.copy(name =  it)) },
        label = { Text(text = stringResource(R.string.common_label_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    LabeledSwitch(
        checked = form.envelope.isBudgeted,
        enabled = form.state is FormState.Active,
        onCheckedChange = { update(form.envelope.copy(isBudgeted = it)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacingMedium)),
    ) {
        Text(
            text = stringResource(R.string.envelope_edit_label_budgeted),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
