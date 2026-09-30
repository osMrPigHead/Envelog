package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.util.HasId
import java.util.UUID

data class Envelope(
    override val id: UUID,
    val name: String,
    val isBudgeted: Boolean,
    val archived: Boolean,
) : HasId
