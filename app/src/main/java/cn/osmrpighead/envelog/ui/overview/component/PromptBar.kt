package cn.osmrpighead.envelog.ui.overview.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.util.MotionTokens
import cn.osmrpighead.envelog.util.annotation.LangPreviews

@Composable
fun PromptBar(
    managing: Boolean,
    onManageButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.prompt_main),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = onManageButtonClick,
        ) {
            Crossfade(
                targetState = managing,
                animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade),
                label = "icon",
            ) { isManaging ->
                Icon(
                    imageVector = if (isManaging) Icon.ListAltFilled else Icon.ListAlt,
                    contentDescription = if (isManaging) stringResource(R.string.prompt_main_manage_finish)
                    else stringResource(R.string.prompt_main_manage),
                )
            }
        }
    }
}

@Preview
@LangPreviews
@Composable
private fun PromptBarPreview() {
    EnvelogTheme {
        var managing by remember { mutableStateOf(false) }
        PromptBar(
            managing = managing,
            onManageButtonClick = { managing = !managing },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
