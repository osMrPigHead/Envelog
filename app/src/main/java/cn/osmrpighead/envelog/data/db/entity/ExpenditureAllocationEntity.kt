package cn.osmrpighead.envelog.data.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.ForeignKey.Companion.RESTRICT
import androidx.room3.Index
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureId

@Entity(
    tableName = "expenditureAllocation",
    primaryKeys = ["expenditureId", "envelopeId"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenditureEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenditureId"],
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
    val expenditureId: ExpenditureId,
    val envelopeId: EnvelopeId,
    val amount: Long,
) {
    companion object {
        fun fromDomain(domain: ExpenditureAllocation): ExpenditureAllocationEntity = ExpenditureAllocationEntity(
            expenditureId = domain.expenditureId,
            envelopeId = domain.envelopeId,
            amount = domain.amount,
        )
    }
}
