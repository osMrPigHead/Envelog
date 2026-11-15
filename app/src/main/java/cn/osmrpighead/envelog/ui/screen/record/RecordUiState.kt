package cn.osmrpighead.envelog.ui.screen.record

import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState

data class RecordUiState(
    val numKeypadHost: NumKeypadHostState,
    val amount: Long,
)
