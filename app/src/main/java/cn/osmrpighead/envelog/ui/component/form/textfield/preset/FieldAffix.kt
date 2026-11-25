package cn.osmrpighead.envelog.ui.component.form.textfield.preset

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.ui.style.fieldPrefix
import cn.osmrpighead.envelog.ui.style.fieldSuffix

@Composable
fun FieldPrefix(text: String, modifier: Modifier = Modifier) {
    if (text.isNotBlank()) Text(text = text, modifier = modifier.fieldPrefix())
}

@Composable
fun FieldSuffix(text: String, modifier: Modifier = Modifier) {
    if (text.isNotBlank()) Text(text = text, modifier = modifier.fieldSuffix())
}
