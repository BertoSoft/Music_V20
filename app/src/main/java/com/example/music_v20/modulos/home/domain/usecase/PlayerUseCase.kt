package com.example.music_v20.modulos.home.domain.usecase

import com.example.music_v20.modulos.home.domain.model.Cancion
import com.example.music_v20.modulos.home.domain.repository.PlayerRepository
import javax.inject.Inject

class PlayerUseCase @Inject constructor(
    private val repository: PlayerRepository
) {

    suspend fun getAllCancionesUseCase(): List<Cancion>{
        return repository.getListaCanciones()
    }
}