package cn.osmrpighead.envelog.di

import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.InMemoryEnvelogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindEnvelogRepository(impl: InMemoryEnvelogRepository): EnvelogRepository
}
