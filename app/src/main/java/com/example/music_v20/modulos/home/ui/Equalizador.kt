package com.example.music_v20.modulos.home.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
// 1. AÑADE ESTA IMPORTACIÓN AL INICIO DE TU EQUALIZADOR.KT
import java.util.LinkedList
import java.util.Queue

// 2. AÑADE ESTA PROPIEDAD DENTRO DE LA CLASE EQUALIZADOR (junto a los otros arrays)
// Esta cola guardará los frames de audio para retrasar su dibujado
private val colaRetardo: Queue<FloatArray> = LinkedList()

// Ajusta este número para sincronizar perfectamente.
// Cada unidad equivale aproximadamente a ~20ms de retraso.
// Para auriculares de cable/emulador: entre 3 y 6 suele ser perfecto.
// Para auriculares Bluetooth: puedes subirlo entre 8 y 12 si notas más retardo.
private val framesDeRetardo = 4



class Equalizador @JvmOverloads constructor(
    miContexto: Context,
    miAtributo: AttributeSet? = null,
    miDefEstiloAtributo: Int = 0
    ): View(
    miContexto,
    miAtributo,
    miDefEstiloAtributo){


    val numeroBarras = 16
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
        val totalBinesFFT = datos.size
        if (totalBinesFFT == 0) return

        // Creamos un array temporal para el frame que acaba de llegar
        val datosFrameActual = FloatArray(numeroBarras)

        // --- ASIGNACIÓN DE BINES (Tu lógica calibrada anterior) ---
        for (iDatos in 0 until numeroBarras) {
            var binInicio = 0
            var binFin = 0

            when (iDatos) {
                0 -> { binInicio = 1; binFin = 2 }
                1 -> { binInicio = 2; binFin = 3 }
                2 -> { binInicio = 3; binFin = 4 }
                3 -> { binInicio = 4; binFin = 6 }
                4 -> { binInicio = 6; binFin = 8 }
                else -> {
                    val factor = (iDatos - 5).toFloat() / (numeroBarras - 6)
                    binInicio = (8 + factor * 18).toInt()
                    binFin = (binInicio + 2 + (factor * 9).toInt())
                }
            }

            val inicioSeguro = binInicio.coerceIn(0, totalBinesFFT - 1)
            val finSeguro = binFin.coerceIn(inicioSeguro + 1, totalBinesFFT)

            var sumaAmplitud = 0f
            var cantidadBines = 0
            for (fftIdx in inicioSeguro until finSeguro) {
                sumaAmplitud += datos[fftIdx]
                cantidadBines++
            }

            datosFrameActual[iDatos] = if (cantidadBines > 0) sumaAmplitud / cantidadBines else 0f
        }

        // --- EL TRUCO DEL RETARDO: COLA FIFO ---
        // Guardamos el frame actual en la cola
        colaRetardo.add(datosFrameActual)

        // Si la cola aún no se ha llenado con los frames de espera, no dibujamos nada
        if (colaRetardo.size < framesDeRetardo) {
            return
        }

        // Cuando la cola se llena, sacamos el frame más antiguo (el retrasado)
        // y lo copiamos a nuestro array de procesamiento
        val frameRetrasado = colaRetardo.poll()
        if (frameRetrasado != null) {
            System.arraycopy(frameRetrasado, 0, datosBrutosBarras, 0, numeroBarras)
        }

        procesaDatos()
        invalidate()
    }

    fun procesaDatos(){
        for (i in 0 until numeroBarras) {
            val datoBruto = datosBrutosBarras[i]
            var valorEcualizado = datoBruto

            when (i) {
                in 0..4 -> { valorEcualizado *= 0.45f }
                in 5..11 -> { valorEcualizado *= 0.22f }
                else -> {
                    val progresoAgudos = (i - 12).toFloat() / 3f
                    valorEcualizado *= (1.8f + progresoAgudos * 5.2f)
                }
            }

            val exponente = when (i) {
                in 0..4 -> 0.65f
                in 5..11 -> 0.65f
                else -> 0.42f
            }

            valorEcualizado = Math.pow(valorEcualizado.toDouble(), exponente.toDouble()).toFloat()
            datosProcesados[i] = valorEcualizado.coerceAtLeast(0.0f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val espacioEntreBarras = 6f
        val anchoTotal = width.toFloat()
        val altoTotal = height.toFloat()

        val anchoBarra = (anchoTotal - (espacioEntreBarras * (numeroBarras - 1))) / numeroBarras
        val maxActual = datosProcesados.maxOrNull() ?: 1.0f

        // Ajuste dinámico del techo visual
        maxHistorico = if (maxActual > maxHistorico) {
            maxActual
        } else {
            maxHistorico * 0.94f + maxActual * 0.06f
        }

        val techo = maxHistorico.coerceAtLeast(0.1f)

        for (i in 0 until numeroBarras) {
            val xI = i * (anchoBarra + espacioEntreBarras)
            val xD = xI + anchoBarra

            val amplitud = datosProcesados[i]
            val alturaObjetivo = ((amplitud / techo) * altoTotal).coerceAtMost(altoTotal)

            // --- FÍSICA ASIMÉTRICA ADAPTATIVA (Control de velocidad y nerviosismo) ---
            if (alturaObjetivo > datosAmortiguados[i]) {
                // Cuando la barra SUBE: Reacción inmediata para todas las barras
                datosAmortiguados[i] = alturaObjetivo
            } else {
                // Cuando la barra BAJA: Filtramos el tiempo de caída según la barra
                when (i) {
                    // Barras 0 a 4 (Graves): Caída muy lenta y pesada (0.93) para quitar el nerviosismo
                    in 0..4 -> {
                        datosAmortiguados[i] = datosAmortiguados[i] * 0.93f + alturaObjetivo * 0.07f
                    }
                    // Barras 5 a 11 (Medios): Caída estándar y equilibrada (0.84)
                    in 5..11 -> {
                        datosAmortiguados[i] = datosAmortiguados[i] * 0.84f + alturaObjetivo * 0.16f
                    }
                    // Barras 12 a 15 (Agudos): Caída ultra rápida (0.60) para que sigan el ritmo al milisegundo
                    else -> {
                        datosAmortiguados[i] = datosAmortiguados[i] * 0.60f + alturaObjetivo * 0.40f
                    }
                }
            }

            val superior = altoTotal - datosAmortiguados[i]
            val inferior = altoTotal

            canvas.drawRect(xI, superior, xD, inferior, pincel)
        }
        }
}