package com.example.music_v20.data.datasource

import com.example.music_v20.modulos.home.domain.model.Cancion
import javax.inject.Inject

class PlayerDataSource @Inject constructor() {

    suspend fun getAllCanciones(): List<Cancion>{
        return emptyList()
    }
}