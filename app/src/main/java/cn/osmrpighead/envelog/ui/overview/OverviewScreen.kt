package cn.osmrpighead.envelog.ui.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.overview.component.EnvelopeCard
import cn.osmrpighead.envelog.ui.overview.component.PromptBar
import cn.osmrpighead.envelog.ui.overview.component.Summary
import java.util.*

@Composable
fun OverviewScreen(
    uiState: OverviewUiState,
    onArchiveClick: (UUID) -> Unit,
    onEditClick: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    var managing by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier) {
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
                    modifier = Modifier
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacingLarge),
                            vertical = dimensionResource(R.dimen.spacingSmall),
                        ),
                )
            }
        }
    }
}
