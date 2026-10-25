package cn.osmrpighead.envelog.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Overview : AppRoute
    @Serializable data class Envelope(@Contextual val id: UUID): AppRoute
}