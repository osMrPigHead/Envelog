package cn.osmrpighead.envelog.ui.screen.envelope2

import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope2.model.EnvelopeFormDraft
import cn.osmrpighead.envelog.ui.screen.envelope2.model.EnvelopeFormDraftErrors
import java.util.UUID

data class EnvelopeUiState(
    val state: FormState<EnvelopeFormDraft, EnvelopeFormDraftErrors>,
    val editingBudget: UUID?,
)
