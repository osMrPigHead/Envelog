package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.form.LabeledSwitch
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextField
import cn.osmrpighead.envelog.ui.icon.Icons
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeField
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeForm
import cn.osmrpighead.envelog.ui.style.formField
import cn.osmrpighead.envelog.ui.style.formSwitch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun EnvelopeForm(
    yearMonth: YearMonth,
    today: LocalDate,
    form: EnvelopeForm,
    dismissed: Boolean,
    update: (EnvelopeField) -> Unit,
    quickset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = form.state is FormState.Active && !dismissed

    Column(modifier = modifier) {
        EnvelogOutlinedTextField(
            value = form.envelope.name,
            enabled = enabled,
            onValueChange = { update(EnvelopeField.Name(it)) },
            label = { Text(text = stringResource(R.string.common_label_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .formField(),
        )

        LabeledSwitch(
            checked = form.envelope.isBudgeted,
            enabled = enabled,
            onCheckedChange = { update(EnvelopeField.IsBudgeted(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .formSwitch(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.envelope_edit_label_budgeted),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )

                TextButton(
                    onClick = quickset,
                    enabled = enabled && form.envelope.isBudgeted && yearMonth >= YearMonth.from(today),
                ) {
                    Icon(
                        imageVector = Icons.Tune,
                        contentDescription = null,
                    )
                    Text(text = stringResource(R.string.envelope_edit_quick_set_title))
                }
            }
        }
    }
}
