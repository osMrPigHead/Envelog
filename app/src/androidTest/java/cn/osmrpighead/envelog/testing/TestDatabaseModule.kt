package cn.osmrpighead.envelog.testing

import android.content.Context
import androidx.room3.Room
import cn.osmrpighead.envelog.core.di.DatabaseModule
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * 端到端测试使用内存 Room 库，避免读写设备上的 envelog.db。
 */
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class],
)
@Module
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideEnvelogDatabase(
        @ApplicationContext context: Context,
    ): EnvelogDatabase = Room.inMemoryDatabaseBuilder(
        context,
        EnvelogDatabase::class.java,
    ).build()
}
