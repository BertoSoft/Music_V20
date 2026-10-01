package com.example.music_v20.modulos.home.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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


    val numeroBarras = 32
    val datosBrutosBarras = FloatArray(numeroBarras)
    val datosProcesados = FloatArray(numeroBarras)
    val pincel = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    var maxHistorico = 1.0f
    // 1. NUEVO: Arreglo para recordar la altura de las barras en el frame anterior
    val datosAmortiguados = FloatArray(numeroBarras)

    fun actualizarBarras(datos: FloatArray){
        // En teroria aqui llegan 126 datos de amplitudes por frecuencias
        if(datos == null) return

        // Vamos a coger los binden 2 de tres y 30 de 4
        var iFFT = 0
        var iDatos = 0
        while (iDatos < numeroBarras) {
            // Definimos cuántos bines agrupar según la posición
            val tamañoGrupo = if (iFFT < 6) 3 else 4

            // Sumamos los elementos del grupo y dividimos por el tamaño
            datosBrutosBarras[iDatos] = (0 until tamañoGrupo).sumOf { datos[iFFT + it].toDouble() }.toFloat() / tamañoGrupo

            iFFT += tamañoGrupo
            iDatos++
        }

        procesaDatos()
        invalidate()
    }

    fun procesaDatos(){
        // Procesa los datos brutos y los guarda en datosProcesados
        for(i in 0 until numeroBarras){
            datosProcesados[i] = (i * 0.15f) * datosBrutosBarras[i]
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if(width == 0 || height == 0) return

        val espacioEntreBarras = 4f // Píxeles de separación
        val anchoTotal = width.toFloat()
        val altoTotal = height.toFloat()

        // Calculamos el ancho de cada barra restando los espacios intermedios
        val anchoBarra = (anchoTotal - (espacioEntreBarras * (numeroBarras - 1))) / numeroBarras

        // Obtenemos el maximo actual
        val maxActual = datosProcesados.maxOrNull() ?: 1.0f

        // Adaptamos el "techo" dinámicamente para que responda bien a canciones altas o bajas
        maxHistorico = if (maxActual > maxHistorico) {
            maxActual
        } else {
            maxHistorico * 0.92f + maxActual * 0.08f // Amortiguación de bajada suave
        }

        // Evitamos división por cero asegurando un mínimo
        val techo = maxHistorico.coerceAtLeast(0.1f)

        for(i in 0 until numeroBarras){
            // Posicion XIzda y Derecha
            val xI = i * (anchoBarra + espacioEntreBarras)
            val xD = xI + anchoBarra

            val amplitud        = datosProcesados[i]
            // Calculamos la altura matemática pura para este instante
            val altura = ((amplitud / techo) * altoTotal).coerceAtMost(altoTotal)
            val alturaObjetivo = altura * 0.85f


            // --- AMORTIGUACIÓN 2: BARRA POR BARRA (La magia visual) ---
            // Si la nueva altura es mayor, la barra sube instantáneamente (golpe de ritmo)
            // Si la nueva altura es menor, cae lentamente usando un factor de amortiguación (0.85f)
            datosAmortiguados[i] = if (alturaObjetivo > datosAmortiguados[i]) {
                alturaObjetivo
            } else {
                datosAmortiguados[i] * 0.85f + alturaObjetivo * 0.15f
            }
            val superior = altoTotal - datosAmortiguados[i]              // El punto más alto de la barra
            val inferior = altoTotal               // La base de la barra (abajo del todo)

            canvas.drawRect(xI, superior, xD, inferior, pincel)

        }


    }


}