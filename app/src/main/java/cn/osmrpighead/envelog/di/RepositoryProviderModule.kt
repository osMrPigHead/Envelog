package cn.osmrpighead.envelog.di

import cn.osmrpighead.envelog.data.repository.EnvelogSeedData
import cn.osmrpighead.envelog.data.repository.InMemoryEnvelogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryProviderModule {
    /**
     * 把每个构建变体提供的 [EnvelogSeedData] 合并后构造单例仓库。
     *
     * debug 变体通过 `DemoSeedModule` 贡献演示数据；release 变体没有贡献，
     * 由 `RepositoryModule` 中的 `@Multibinds` 提供空集合。
     */
    @Provides
    @Singleton
    fun provideInMemoryEnvelogRepository(
        seedData: Set<@JvmSuppressWildcards EnvelogSeedData>,
    ): InMemoryEnvelogRepository = InMemoryEnvelogRepository(
        envelopes = seedData.flatMap { it.envelopes },
        budgets = seedData.flatMap { it.budgets },
        transactions = seedData.flatMap { it.transactions },
        allocations = seedData.flatMap { it.allocations },
    )
}
