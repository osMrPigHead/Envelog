package cn.osmrpighead.envelog.ui.navigation

import java.util.UUID

sealed interface Screen {
    data object Overview : Screen
    data class Envelope(val id: UUID): Screen
}