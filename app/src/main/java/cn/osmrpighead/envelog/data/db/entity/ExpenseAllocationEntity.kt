package cn.osmrpighead.envelog.data.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.ForeignKey.Companion.RESTRICT
import androidx.room3.Index
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenseAllocation
import cn.osmrpighead.envelog.data.model.ExpenseId

@Entity(
    tableName = "expenseAllocation",
    primaryKeys = ["expenseId", "envelopeId"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = CASCADE,
            onUpdate = CASCADE,
        ),
        ForeignKey(
            entity = EnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["envelopeId"],
            onDelete = RESTRICT,
            onUpdate = CASCADE,
        ),
    ],
    indices = [Index("envelopeId")]
)
data class ExpenseAllocationEntity(
    val expenseId: ExpenseId,
    val envelopeId: EnvelopeId,
    val amount: Long,
)
