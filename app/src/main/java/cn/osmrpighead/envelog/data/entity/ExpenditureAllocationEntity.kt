package cn.osmrpighead.envelog.data.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.ForeignKey.Companion.RESTRICT
import androidx.room3.Index
import java.util.UUID

@Entity(
    tableName = "enpenditure_allocations",
    primaryKeys = ["transactionId", "envelopeId"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
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
data class ExpenditureAllocationEntity(
    val transactionId: UUID,
    val envelopeId: UUID,
    val amount: Long,
)
