package com.example.music_v20.modulos.home.domain.repository

import com.example.music_v20.modulos.home.domain.model.Cancion
import javax.inject.Inject

interface PlayerRepository  {

    suspend fun getListaCanciones(): List<Cancion>
}