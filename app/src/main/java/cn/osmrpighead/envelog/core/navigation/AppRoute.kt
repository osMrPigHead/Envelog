package cn.osmrpighead.envelog.core.navigation

import androidx.navigation3.runtime.NavKey
import cn.osmrpighead.envelog.data.model.EnvelopeId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Overview : AppRoute
    @Serializable data class Record(val id: EnvelopeId): AppRoute
    @Serializable data class Envelope(val id: NavigationTarget<@Contextual EnvelopeId>): AppRoute
}