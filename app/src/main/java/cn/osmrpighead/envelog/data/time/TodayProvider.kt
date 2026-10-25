package cn.osmrpighead.envelog.data.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodayProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val _today = MutableStateFlow(LocalDate.now(clock))
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private var receiver: BroadcastReceiver? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) = unregisterReceiver()
                override fun onStart(owner: LifecycleOwner) {
                    _today.value = LocalDate.now(clock)
                    registerReceiver()
                }
            },
        )
    }

    private fun registerReceiver() {
        if (receiver != null) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                _today.value = LocalDate.now(clock)
            }
        }.also { r ->
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            context.registerReceiver(r, filter)
        }
    }

    private fun unregisterReceiver() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
}