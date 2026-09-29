package com.example.music_v20.modulos.home.domain.usecase

import com.example.music_v20.modulos.home.domain.model.Cancion
import javax.inject.Inject

class PlayerUseCase @Inject constructor() {

    suspend fun getAllCancionesUseCase(): List<Cancion>{
        return emptyList<Cancion>()
    }
}