package cn.osmrpighead.envelog.ui.preview

import androidx.compose.ui.tooling.preview.Preview

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@Preview(name = "English", locale = "en", showBackground = true)
@Preview(name = "简体中文", locale = "zh-rCN", showBackground = true)
annotation class LangPreview
