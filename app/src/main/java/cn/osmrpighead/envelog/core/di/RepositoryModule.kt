package cn.osmrpighead.envelog.core.di

import cn.osmrpighead.envelog.data.db.DatabaseSeeder
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.RoomEnvelogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindEnvelogRepository(impl: RoomEnvelogRepository): EnvelogRepository

    @Multibinds
    abstract fun databaseSeeders(): Set<DatabaseSeeder>
}
