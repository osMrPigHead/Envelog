package cn.osmrpighead.envelog.di

import cn.osmrpighead.envelog.data.repository.DemoSeedData
import cn.osmrpighead.envelog.data.repository.EnvelogSeedData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.time.Clock
import java.time.LocalDate

/**
 * 只在 debug 变体中装配演示数据；release 没有这个 module，仓库会以空数据启动。
 */
@Module
@InstallIn(SingletonComponent::class)
object DemoSeedModule {
    @Provides
    @IntoSet
    fun provideDemoSeedData(clock: Clock): EnvelogSeedData =
        DemoSeedData.build(LocalDate.now(clock), clock.zone)
}
