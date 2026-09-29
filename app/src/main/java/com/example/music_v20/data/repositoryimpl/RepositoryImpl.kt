package com.example.music_v20.data.repositoryimpl

import com.example.music_v20.core.di.IoDispatcher
import com.example.music_v20.data.datasource.PlayerDataSource
import com.example.music_v20.data.mappers.toDomain
import com.example.music_v20.data.model.CancionDto
import com.example.music_v20.modulos.home.domain.model.Cancion
import com.example.music_v20.modulos.home.domain.repository.PlayerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RepositoryImpl @Inject constructor(
    private val dataSource: PlayerDataSource,
    @IoDispatcher private val coroutine: CoroutineDispatcher
): PlayerRepository {
    override suspend fun getListaCanciones(): List<Cancion> {
        return withContext(coroutine){
            val listaDto = dataSource.getAllCanciones()
            val listaDomain = listaDto.map { it.toDomain() }
            return@withContext listaDomain
        }
    }
}