package cn.osmrpighead.envelog.di

import cn.osmrpighead.envelog.data.db.DatabaseSeeder
import cn.osmrpighead.envelog.data.repository.DemoDatabaseSeeder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * 只在 debug 变体中装配演示数据；release 没有这个 module，数据库保持为空。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DemoSeedModule {
    @Binds
    @IntoSet
    abstract fun bindDemoDatabaseSeeder(seeder: DemoDatabaseSeeder): DatabaseSeeder
}
