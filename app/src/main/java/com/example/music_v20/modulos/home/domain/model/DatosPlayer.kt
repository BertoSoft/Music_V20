package com.example.music_v20.modulos.home.domain.model

data class Cancion(
    val id: Int,
    val id_android: Long,
    val titulo: String,
    val autor: String,
    val duracion: Long,
    val ruta: String
)
