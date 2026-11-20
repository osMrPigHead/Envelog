package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R

@Composable
fun OptionalTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit,
) {
    TextField(
        state = state,
        placeholder = { ProvideTextStyle(MaterialTheme.typography.bodyMedium, placeholder) },
        textStyle = MaterialTheme.typography.bodyMedium,
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.outline,
        ),
        contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
            top = dimensionResource(R.dimen.optionalFieldVerticalPadding),
            bottom = dimensionResource(R.dimen.optionalFieldVerticalPadding),
        ),
        modifier = modifier.heightIn(dimensionResource(R.dimen.minTouchTarget)),
    )
}
