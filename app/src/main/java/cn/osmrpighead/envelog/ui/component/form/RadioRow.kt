package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cn.osmrpighead.envelog.ui.component.MeasureInvisible
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextField
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextFieldDefaults
import cn.osmrpighead.envelog.ui.component.form.textfield.minimizedLabelHalfHeight

@Composable
fun RadioRow(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    localEnabled: Boolean = LocalFormEnabled.current,
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    labeled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val enabled = enabled && localEnabled
    var height by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    MeasureInvisible({ with(density) { height = it.toDp() } }) { EnvelogOutlinedTextField("", {}) }

    Row(
        modifier = modifier
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .height(IntrinsicSize.Min),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(
                    if (labeled) Modifier.padding(top = minimizedLabelHalfHeight())
                    else Modifier
                )
                .height(height),
        ) { RadioButton(selected = selected, enabled = enabled, onClick = onClick) }
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = verticalAlignment,
        ) {
            ProvideForm(enabled = enabled, active = selected) {
                if (enabled && !selected) EnvelogOutlinedTextFieldDefaults.ProvideUnselected { content() }
                else content()
            }
        }
    }
}
