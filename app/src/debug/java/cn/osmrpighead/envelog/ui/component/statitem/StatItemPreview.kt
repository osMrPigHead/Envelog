package cn.osmrpighead.envelog.ui.component.statitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@LangPreview
@Composable
private fun StatItemPreview() {
    EnvelogTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            StatItemSample("ExtremeCenterStart", StatItemStyle.ExtremeCenterStart)
            StatItemSample("ExtremeCenterEnd", StatItemStyle.ExtremeCenterEnd)
            StatItemSample("LargeCenterStart", StatItemStyle.LargeCenterStart)
            StatItemSample("LargeCenterEnd", StatItemStyle.LargeCenterEnd)
            StatItemSample("MediumCenterStart", StatItemStyle.MediumCenterStart)
            StatItemSample("MediumCenterEnd", StatItemStyle.MediumCenterEnd)
        }
    }
}

@Composable
private fun StatItemSample(
    name: String,
    style: StatItemStyle,
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingThin))) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        StatItem(
            style = style,
            content = StatItemContent(
                title = StatItemSlot.Text("Title"),
                value = StatItemSlot.Text("1,234.56"),
                caption = StatItemSlot.Text("Caption"),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
