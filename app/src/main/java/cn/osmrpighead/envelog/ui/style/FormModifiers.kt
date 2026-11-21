package cn.osmrpighead.envelog.ui.style

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R

@Composable
fun Modifier.formField(): Modifier = this
    .fillMaxWidth()
    .formFieldPadding()

@Composable
fun Modifier.formFieldPadding(): Modifier = padding(bottom = dimensionResource(R.dimen.space12))

@Composable
fun Modifier.subtleField(): Modifier = heightIn(dimensionResource(R.dimen.minTouchTarget))

@Composable
fun Modifier.formSwitch(): Modifier = this
    .fillMaxWidth()
    .formSwitchPadding()

@Composable
fun Modifier.formSwitchPadding(): Modifier = padding(bottom = dimensionResource(R.dimen.space4))
