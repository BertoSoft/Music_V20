package com.example.music_v20.data.model

import javax.inject.Inject

data class CancionDto(
    val id: Int,
    val id_android: Long?,
    val artist: String?,
    val title: String?,
    val duration: Long?,
    val data: String?
)
