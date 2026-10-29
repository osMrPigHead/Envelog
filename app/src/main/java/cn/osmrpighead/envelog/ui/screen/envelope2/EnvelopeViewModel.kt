package cn.osmrpighead.envelog.ui.screen.envelope2

import androidx.lifecycle.ViewModel
import cn.osmrpighead.envelog.navigation.AppRoute
import cn.osmrpighead.envelog.ui.model.AppStateHolder

class EnvelopeViewModel(
    private val route: AppRoute.Envelope,
    appState: AppStateHolder
) : ViewModel() {
    private val yearMonth = appState.yearMonth
    
}
