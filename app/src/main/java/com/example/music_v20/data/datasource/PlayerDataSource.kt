package com.example.music_v20.data.datasource

import android.content.Context
import android.provider.MediaStore
import com.example.music_v20.data.model.CancionDto
import com.example.music_v20.modulos.home.domain.model.Cancion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PlayerDataSource @Inject constructor(
    @ApplicationContext private val miContexto: Context
) {

    suspend fun getAllCanciones(): List<CancionDto>{
        val tamanoMin = (1024L * 1024L)
        var id = 0
        val listaDto = mutableListOf<CancionDto>()
        val uriMusica = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val campos = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )
        val filtros = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val orden = "${MediaStore.Audio.Media.TITLE} ASC"

        miContexto.contentResolver.query(
            uriMusica,
            campos,
            filtros,
            null,
            orden
        )?.use { cursor ->
            // Indice de cada columna
            val id_androidCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA) // Sin comillas
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()){
                val id_android = cursor.getLong(id_androidCol)
                val artist = cursor.getString(artistCol)
                val title = cursor.getString(titleCol)
                val duration = cursor.getLong(durationCol)
                val data = cursor.getString(dataCol)
                val size = cursor.getLong(sizeCol)

                if(size > tamanoMin){
                    var existeCancion: CancionDto? = null
                    existeCancion = listaDto.find {
                        it.duration == duration &&
                        it.artist == artist &&
                        it.title == title
                        }
                    if(existeCancion == null){
                        listaDto.add(CancionDto(
                            id = id,
                            id_android = id_android,
                            artist = artist,
                            title = title,
                            duration = duration,
                            data = data
                        ))
                        id++
                    }
                }
            }
        }
        return listaDto
    }

}