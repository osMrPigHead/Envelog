package cn.osmrpighead.envelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import cn.osmrpighead.envelog.core.navigation.AppNavDisplay
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val backStack = rememberNavBackStack(AppRoute.Overview) as NavBackStack<AppRoute>
            EnvelogTheme {
                AppNavDisplay(backStack)
            }
        }
    }
}
