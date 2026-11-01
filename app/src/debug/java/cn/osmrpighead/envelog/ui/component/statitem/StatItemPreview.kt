package cn.osmrpighead.envelog.ui.component.statitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
private fun StatItemExtremePreview() = StatItemStylePreview {
    StatItemSample("ExtremeCenterStart", StatItemStyle.ExtremeCenterStart)
    StatItemSample("ExtremeCenter", StatItemStyle.ExtremeCenter)
    StatItemSample("ExtremeCenterEnd", StatItemStyle.ExtremeCenterEnd)
}

@LangPreview
@Composable
private fun StatItemLargePreview() = StatItemStylePreview {
    StatItemSample("LargeCenterStart", StatItemStyle.LargeCenterStart)
    StatItemSample("LargeCenter", StatItemStyle.LargeCenter)
    StatItemSample("LargeCenterEnd", StatItemStyle.LargeCenterEnd)
}

@LangPreview
@Composable
private fun StatItemMediumPreview() = StatItemStylePreview {
    StatItemSample("MediumCenterStart", StatItemStyle.MediumCenterStart)
    StatItemSample("MediumCenter", StatItemStyle.MediumCenter)
    StatItemSample("MediumCenterEnd", StatItemStyle.MediumCenterEnd)
}

@LangPreview
@Composable
private fun StatItemSmallPreview() = StatItemStylePreview {
    StatItemSample("SmallCenterStart", StatItemStyle.SmallCenterStart)
    StatItemSample("SmallCenter", StatItemStyle.SmallCenter)
    StatItemSample("SmallCenterEnd", StatItemStyle.SmallCenterEnd)
}

@Composable
private fun StatItemStylePreview(content: @Composable ColumnScope.() -> Unit) {
    EnvelogTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
            content = content,
        )
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
