package cn.osmrpighead.envelog.testing

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher

/**
 * 只匹配 Button 语义节点，避免与同样带文本和点击动作的只读输入框混淆。
 */
fun hasButtonRole(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
