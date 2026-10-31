package cn.osmrpighead.envelog.core.di

import cn.osmrpighead.envelog.data.repository_old.EnvelogRepository
import cn.osmrpighead.envelog.data.repository_old.EnvelogSeedData
import cn.osmrpighead.envelog.data.repository_old.InMemoryEnvelogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindEnvelogRepository(impl: InMemoryEnvelogRepository): EnvelogRepository

    @Multibinds
    abstract fun envelogSeedData(): Set<EnvelogSeedData>
}
