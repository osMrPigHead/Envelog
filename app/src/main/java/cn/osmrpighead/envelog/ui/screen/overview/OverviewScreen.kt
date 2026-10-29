package cn.osmrpighead.envelog.ui.screen.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.screen.overview.component.EnvelopeCard
import cn.osmrpighead.envelog.ui.screen.overview.component.PromptBar
import cn.osmrpighead.envelog.ui.screen.overview.component.Summary
import cn.osmrpighead.envelog.ui.theme.MotionTokens
import java.util.*

@Composable
fun OverviewScreen(
    uiState: OverviewUiState,
    onArchiveClick: (UUID) -> Unit,
    onEditClick: (UUID) -> Unit,
    onNewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var managing by rememberSaveable { mutableStateOf(false) }
    Scaffold { innerPadding ->
        Column(modifier = modifier.padding(innerPadding)) {
            Summary(
                yearMonth = uiState.yearMonth,
                items = uiState.summaryItems,
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.spacingBold))
                    .padding(
                        top = dimensionResource(R.dimen.spacingBold),
                        bottom = dimensionResource(R.dimen.spacingMedium),
                    ),
            )
            PromptBar(
                managing = managing,
                onManageButtonClick = { managing = !managing },
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacingBold)),
            )
            val cardModifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacingLarge),
                vertical = dimensionResource(R.dimen.spacingSmall),
            )
            LazyColumn {
                items(uiState.envelopes, key = { it.id }) {
                    EnvelopeCard(
                        model = it,
                        managing = managing,
                        diffColor = { diffAmount ->
                            if (diffAmount < 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        },  // TODO: User rule
                        onArchiveClick = { onArchiveClick(it.id) },
                        onEditClick = { onEditClick(it.id) },
                        modifier = cardModifier,
                    )
                }
                item {
                    AnimatedContent(
                        targetState = uiState.envelopes.isEmpty() || managing,
                        contentAlignment = Alignment.CenterEnd,
                        transitionSpec = {
                            val direction = if (targetState) 1 else -1
                            val enter = slideInVertically(
                                animationSpec = tween(durationMillis = MotionTokens.DurationSwitchSlide),
                                initialOffsetY = { height -> -direction * height / 4 },
                            ) + fadeIn(animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade))
                            val exit = slideOutVertically(
                                animationSpec = tween(durationMillis = MotionTokens.DurationSwitchSlide),
                                targetOffsetY = { height -> direction * height / 4 },
                            ) + fadeOut(animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade))
                            (enter togetherWith exit).using(null)
                        },
                    ) {
                        if (it) Card(modifier = cardModifier.clickable(onClick = onNewClick)) {
                            Row(
                                modifier = Modifier
                                    .padding(dimensionResource(R.dimen.spacingBold))
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icon.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(
                                            with(LocalDensity.current) {
                                                MaterialTheme.typography.titleLarge.lineHeight.toDp()
                                            },
                                        )
                                        .padding(end = dimensionResource(R.dimen.spacingThin)),
                                )
                                Text(
                                    text = stringResource(R.string.envelope_manage_new),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
