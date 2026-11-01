package cn.osmrpighead.envelog.ui.screen.overview.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.theme.MotionTokens

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


