package com.example.music_v20.modulos.home.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View

class Equalizador @JvmOverloads constructor(
    miContexto: Context,
    miAtributo: AttributeSet? = null,
    miDefEstiloAtributo: Int = 0
    ): View(
    miContexto,
    miAtributo,
    miDefEstiloAtributo){

    fun actualizarBarras(datos: FloatArray){

        // En teroria aqui llegan 126 datos de amplitudes por frecuencias
        if(datos == null) return

    }


}