package cn.osmrpighead.envelog.ui.screen.envelope2.model

import java.util.UUID

data class EnvelopeFormDraftErrors(
    val name: Boolean,
    val budgets: List<UUID>,
)