package cn.osmrpighead.envelog

import android.app.Application
import android.util.Log
import cn.osmrpighead.envelog.data.db.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EnvelogApplication : Application() {
    @Inject
    lateinit var databaseSeeders: Set<@JvmSuppressWildcards DatabaseSeeder>

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (databaseSeeders.isEmpty()) return

        applicationScope.launch {
            databaseSeeders.forEach { seeder ->
                runCatching { seeder.seedIfNeeded() }
                    .onFailure { Log.e("EnvelogSeed", "Failed to seed database", it) }
            }
        }
    }
}
