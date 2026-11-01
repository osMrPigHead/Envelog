package cn.osmrpighead.envelog.data.db.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import cn.osmrpighead.envelog.data.model.EnvelopeId
import java.time.Instant

@Entity(tableName = "envelope")
data class EnvelopeEntity(
    @PrimaryKey val id: EnvelopeId,
    val name: String,
    val isBudgeted: Boolean,
    val archivedAt: Instant?,
)
