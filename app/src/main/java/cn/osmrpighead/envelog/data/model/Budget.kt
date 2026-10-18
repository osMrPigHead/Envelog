package cn.osmrpighead.envelog.data.model

import java.time.LocalDate
import java.util.UUID

data class Budget(
    override val id: UUID,
    val envelopeId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val amount: Long,
) : HasId
