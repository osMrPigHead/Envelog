package cn.osmrpighead.envelog.ui.screen.envelope.feature

import cn.osmrpighead.envelog.ui.model.FormState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 表单 Busy 态迁移：快照读取，仅当 [formStateOf] 为 Active（`allowError` 为 false 时还要求无错误）
 * 才置入 Busy 并返回快照，否则返回 null。dismissed 等额外守卫并入 [formStateOf]（返回 null 即拒绝）。
 */
internal fun <S : Any> MutableStateFlow<S?>.requestBusy(
    allowError: Boolean,
    formStateOf: (S) -> FormState<*>?,
    toBusy: (S) -> S,
): S? {
    val snapshot = value ?: return null
    var ok = false
    update { s ->
        val form = s?.let(formStateOf)
        if (s == null || form !is FormState.Active || (!allowError && form.error != null)) return@update s
        ok = true
        toBusy(s)
    }
    return if (ok) snapshot else null
}

/** 仅当 [formStateOf] 为 Busy 时写回 [transform]，否则保持不变。 */
internal fun <S : Any> MutableStateFlow<S?>.releaseBusy(
    formStateOf: (S) -> FormState<*>?,
    transform: (S) -> S,
) {
    update { s ->
        if (s == null || s.let(formStateOf) != FormState.Busy) return@update s
        transform(s)
    }
}
