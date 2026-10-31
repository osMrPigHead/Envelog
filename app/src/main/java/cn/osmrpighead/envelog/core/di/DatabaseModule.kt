package cn.osmrpighead.envelog.core.di

import android.content.Context
import androidx.room3.Room
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideEnvelogDatabase(
        @ApplicationContext context: Context,
    ): EnvelogDatabase = Room.databaseBuilder<EnvelogDatabase>(
        context = context,
        name = "envelog.db",
    ).build()
}
