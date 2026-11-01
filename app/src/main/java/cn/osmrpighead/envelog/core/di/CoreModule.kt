package cn.osmrpighead.envelog.core.di

import cn.osmrpighead.envelog.util.UUIDGenerator
import com.fasterxml.uuid.NoArgGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideNoArgGenerator(): NoArgGenerator = UUIDGenerator.instance
}