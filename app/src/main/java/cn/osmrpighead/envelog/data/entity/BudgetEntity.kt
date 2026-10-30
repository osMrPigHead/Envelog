package cn.osmrpighead.envelog.data.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.Index
import androidx.room3.PrimaryKey
import cn.osmrpighead.envelog.core.time.DatePeriod
import java.util.UUID

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = EnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["envelopeId"],
            onDelete = CASCADE,
            onUpdate = CASCADE,
        )
    ],
    indices = [Index("envelopeId")]
)
data class BudgetEntity(
    @PrimaryKey override val id: UUID,
    val envelopeId: UUID,
    @Embedded val period: DatePeriod,
    val amount: Long,
) : HasId
