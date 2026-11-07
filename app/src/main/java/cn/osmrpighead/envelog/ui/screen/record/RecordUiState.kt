package cn.osmrpighead.envelog.ui.screen.record

import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer

data class RecordUiState(
    val numKeypadHost: NumKeypadHostState,
    val amount: Long,
    val amountConsumer: AmountConsumer,
    val fieldId: Any,
)
