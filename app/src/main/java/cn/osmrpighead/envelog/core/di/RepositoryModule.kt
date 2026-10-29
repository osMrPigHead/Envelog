package cn.osmrpighead.envelog.core.di

import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.EnvelogSeedData
import cn.osmrpighead.envelog.data.repository.InMemoryEnvelogRepository
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
