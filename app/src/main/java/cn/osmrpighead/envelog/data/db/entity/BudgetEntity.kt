package cn.osmrpighead.envelog.data.db.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.ForeignKey.Companion.RESTRICT
import androidx.room3.Index
import androidx.room3.PrimaryKey
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import java.time.Instant
import java.time.YearMonth

@Entity(
    tableName = "budget",
    foreignKeys = [
        ForeignKey(
            entity = EnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["envelopeId"],
            onDelete = RESTRICT,
            onUpdate = CASCADE,
        )
    ],
    indices = [Index("envelopeId")]
)
data class BudgetEntity(
    @PrimaryKey val id: BudgetId,
    val envelopeId: EnvelopeId,
    @Embedded val dateSpan: HalfOpenDateSpan,
    val attributedMonth: YearMonth,
    val amount: Long,
    val deletedAt: Instant?,
)
