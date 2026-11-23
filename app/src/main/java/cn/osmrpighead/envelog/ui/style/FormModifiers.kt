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
fun Modifier.formFieldPadding(): Modifier = formRowPadding().padding(horizontal = dimensionResource(R.dimen.space8))

@Composable
fun Modifier.formRow(): Modifier = this
    .fillMaxWidth()
    .formRowPadding()

@Composable
fun Modifier.formRowPadding(): Modifier = padding(bottom = dimensionResource(R.dimen.space12))

@Composable
fun Modifier.fieldPrefix(): Modifier = padding(end = dimensionResource(R.dimen.space4))

@Composable
fun Modifier.fieldSuffix(): Modifier = padding(start = dimensionResource(R.dimen.space4))

@Composable
fun Modifier.formSupportingText(): Modifier = this
    .fillMaxWidth()
    .formSupportingTextPadding()

@Composable
fun Modifier.formSupportingTextPadding(): Modifier = this
    .padding(horizontal = dimensionResource(R.dimen.space16))
    .padding(bottom = dimensionResource(R.dimen.space8))

@Composable
fun Modifier.inlineFormField(): Modifier = padding(end = dimensionResource(R.dimen.space8))

@Composable
fun Modifier.subtleField(): Modifier = heightIn(dimensionResource(R.dimen.minTouchTarget))

@Composable
fun Modifier.formSwitch(): Modifier = this
    .fillMaxWidth()
    .formSwitchPadding()

@Composable
fun Modifier.formSwitchPadding(): Modifier = padding(bottom = dimensionResource(R.dimen.space4))
