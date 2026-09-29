package com.example.music_v20.core.di

import com.example.music_v20.data.repositoryimpl.RepositoryImpl
import com.example.music_v20.modulos.home.domain.repository.PlayerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModulo {

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(
        repositoryImpl: RepositoryImpl
    ): PlayerRepository
}