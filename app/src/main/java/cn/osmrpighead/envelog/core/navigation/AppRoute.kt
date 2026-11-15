package cn.osmrpighead.envelog.core.navigation

import androidx.navigation3.runtime.NavKey
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.util.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@JvmInline
@Serializable
value class NavId(val value: @Serializable(with = UUIDSerializer::class) UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate() = NavId(UUID.randomUUID())
    }
}

@Serializable
sealed interface AppRoute : NavKey {
    val navId: NavId

    @Serializable
    data class Overview(override val navId: NavId) : AppRoute {
        constructor() : this(NavId.generate())
    }

    @Serializable
    data class Record(override val navId: NavId, val id: EnvelopeId) : AppRoute {
        constructor(id: EnvelopeId) : this(NavId.generate(), id)
    }

    @Serializable
    data class Envelope(
        override val navId: NavId, val id: NavigationTarget<EnvelopeId>,
    ) : AppRoute {
        constructor(id: NavigationTarget<EnvelopeId>) : this(NavId.generate(), id)
    }
}