package cn.osmrpighead.envelog.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@PreviewScreenSizes
@Composable
private fun LabeledBoxPreview() {
    EnvelogTheme(dynamicColor = false) {
        LabeledBox(
            label = "Budget",
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
        ) {
            Text(
                text = "2026/9/1 ~ 2026/9/30",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacingThin)))
            Text(
                text = "Placeholder content used to check how the label notch and border follow " +
                    "the content width on different screen sizes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
