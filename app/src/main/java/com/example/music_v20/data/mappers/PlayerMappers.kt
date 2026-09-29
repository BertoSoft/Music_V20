package com.example.music_v20.data.mappers

import com.example.music_v20.data.model.CancionDto
import com.example.music_v20.modulos.home.domain.model.Cancion

fun CancionDto.toDomain(): Cancion {
    return Cancion(
        id = this.id,
        id_android = this.id_android ?: 0L,
        titulo = this.title ?: "",
        autor = this.artist ?: "",
        duracion = this.duration ?: 0L,
        ruta = this.data.toString()
    )
}