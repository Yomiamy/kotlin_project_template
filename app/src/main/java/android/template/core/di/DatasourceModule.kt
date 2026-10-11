package android.template.core.di

import android.template.infra.network.NetworkDataSource
import android.template.infra.network.TaskNetworkDataSrc
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DatasourceModule {

    @Singleton
    @Binds
    fun bindNetworkDataSource(impl: TaskNetworkDataSrc): NetworkDataSource
}