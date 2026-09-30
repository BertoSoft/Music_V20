package com.example.music_v20.modulos.home.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.music_v20.databinding.ActivityPlayerBinding
import com.example.music_v20.modulos.home.domain.extensions.toMinSeg
import com.example.music_v20.modulos.home.ui.adapter.PlayerAdapter
import com.example.music_v20.modulos.home.ui.viewmodel.ExoPlayerEstado
import com.example.music_v20.modulos.home.ui.viewmodel.ExoPlayerUiEstado
import com.example.music_v20.modulos.home.ui.viewmodel.PlayerUiEstado
import com.example.music_v20.modulos.home.ui.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()
    private val miAdaptador: PlayerAdapter by lazy { PlayerAdapter() }
    private var exoPlayer: ExoPlayer? = null
    private var visualizadorAudio: android.media.audiofx.Visualizer? = null
    private var trabajoProgreso: Job? = null
    private var estaUsuarioMoviendoBarra = false

    // 1. Un ÚNICO lanzador simplificado para todos los permisos
    private val lanzadorPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        // Comprobamos si el permiso de música fue aceptado
        val permisoMusica = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (resultados[permisoMusica] == true) {
            // Permiso concedido -> Cargamos las canciones
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUi()

    }

    private fun initUi() {
        initExoPlayer()
        initRv()
        initListeners()
        initObservers()
    }

    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            // Listener para detectar el fin de la cancion
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                        viewModel.cancionEnd(estaUsuarioMoviendoBarra)
                    }
                }
            })
        }
    }

    private fun initVisualizador(idAudioSesion: Int) {
        try {
            visualizadorAudio = android.media.audiofx.Visualizer(idAudioSesion).apply {
                // Fijamos el tamaño directamente a 256 de forma segura
                captureSize = 256

                setDataCaptureListener(object :
                    android.media.audiofx.Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        p0: Visualizer?,
                        p1: ByteArray?,
                        p2: Int
                    ) {
                        TODO("Not yet implemented")
                    }

                    override fun onFftDataCapture(
                        p0: Visualizer?,
                        fft: ByteArray?,
                        sampleRate: Int
                    ) {
                        if (fft != null) procesaDatosFFT(fft, sampleRate)
                    }
                }, android.media.audiofx.Visualizer.getMaxCaptureRate() / 2, false, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initRv() {
        with(binding.rvCanciones) {
            layoutManager = LinearLayoutManager(this@PlayerActivity)
            adapter = miAdaptador
            setHasFixedSize(true)
        }

        miAdaptador.onItemClick = { cancion ->
            viewModel.setCancionActual(cancion)
        }
    }

    private fun initListeners() {
        with(binding) {
            btnPlay.setOnClickListener {
                viewModel.playClick()
            }
            btnAdelante.setOnClickListener {
                viewModel.adelanteClick()
            }
            btnAtras.setOnClickListener {
                viewModel.atrasClick()
            }
            seekBarraProgreso.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seelBarra: SeekBar?,
                    posicion: Int,
                    movidoPorUsuario: Boolean
                ) {
                    // Cambia el progreso
                    txtProgreso.text = posicion.toLong().toMinSeg()
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    //el usuario empezo a tocar la barra
                    estaUsuarioMoviendoBarra = true
                    if (exoPlayer?.isPlaying == true) {
                        viewModel.playClick()
                    }
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    // Elusuario solto la barrra
                    estaUsuarioMoviendoBarra = false
                    val newPos = p0?.progress?.toLong()

                    if (newPos != null) {
                        exoPlayer?.seekTo(newPos)
                        if (exoPlayer?.isPlaying == false) {
                            viewModel.playClick()
                        }
                    }
                }
            })
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { estadoUi ->
                    dibujaUi(estadoUi)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.player.collect { estadoPlayer ->
                    gestionaPlayer(estadoPlayer)
                }
            }
        }
    }

    fun dibujaUi(estado: PlayerUiEstado) {
        with(binding) {

            // 0.- Barra de Progreso, se activa cuando isCargando es cierto
            if (estado.isCargando) {
                pbCargando.visibility = View.VISIBLE
                rvCanciones.alpha = 0.5F
            } else {
                pbCargando.visibility = View.GONE
                rvCanciones.alpha = 1.0F
            }

            // 1.- RecyclerView entra cuando la lista del adaptador != estado.lista
            if (miAdaptador.currentList != estado.listaCanciones) {
                miAdaptador.submitList(estado.listaCanciones)
            }

            // 2.- Cancion Actual y duracion, cuando existe cancionActual y titulo != txtCancion.text
            if (estado.cancionActual != null && estado.cancionActual.titulo != txtCancion.text) {
                txtCancion.text = estado.cancionActual.titulo
                txtCancion.tag = txtCancion.id
                txtDuracion.text = estado.cancionActual.duracion.toMinSeg()
                seekBarraProgreso.max = estado.cancionActual.duracion.toInt()
                miAdaptador.setSeleccion(estado.cancionActual.id)
                seekBarraProgreso.progress = 0
                txtProgreso.text = "00:00"
            }

            // 5.- Mensajes de error
            if (estado.msgError != null) {
                Toast.makeText(
                    this@PlayerActivity,
                    estado.msgError,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun gestionaPlayer(estado: ExoPlayerUiEstado) {
        if (estado.cancionActual == null) return

        when (estado.estadoPlayer) {

            //exoPlayer()
            ExoPlayerEstado.Play -> {
                val rutaNueva = estado.cancionActual.ruta ?: ""

                binding.btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                binding.visualizadorEQ.visibility = View.VISIBLE

                if (exoPlayer?.currentMediaItem == null ||
                    exoPlayer?.currentMediaItem?.mediaId != estado.cancionActual.id_android.toString()
                ) {

                    val mediaItem = MediaItem.Builder()
                        .setUri(rutaNueva)
                        .setMediaId(estado.cancionActual.id_android.toString()) // Guardamos el ID para identificarla
                        .build()

                    exoPlayer?.setMediaItem(mediaItem)
                    exoPlayer?.prepare()
                }
                exoPlayer?.play()
                iniciarBucleProgreso();

                // Vinculamos el visulizadoral Id de sesion de AudioPlayer
                val idAudioSesion = exoPlayer?.audioSessionId ?: 0
                if (idAudioSesion != 0) {
                    initVisualizador(idAudioSesion)
                }
                visualizadorAudio?.enabled = true
            }

            ExoPlayerEstado.Pause -> {
                // Sincronización de UI: Botón central cambia a PLAY y pausamos ecualizador
                binding.btnPlay.setImageResource(android.R.drawable.ic_media_play)
                binding.visualizadorEQ.visibility = View.INVISIBLE

                // Lógica Física: Pausamos ExoPlayer y congelamos el hilo de la barra
                exoPlayer?.pause()
                detenerBucleProgreso()

                visualizadorAudio?.enabled = false
            }

            ExoPlayerEstado.Stop -> {
                binding.btnPlay.setImageResource(android.R.drawable.ic_media_play)
                binding.visualizadorEQ.visibility = View.INVISIBLE

                exoPlayer?.stop()
                detenerBucleProgreso()

                // Reseteamos valores visuales a cero
                binding.seekBarraProgreso.progress = 0
                binding.txtProgreso.text = "00:00"

                visualizadorAudio?.enabled = false
            }
        }
    }

    private fun procesaDatosFFT(fft: ByteArray, sampleRate: Int) {
        val n = fft.size

        // Creamos el array restando 1 posición, ya que vamos a ignorar los datos 0 y 1
        val datos = FloatArray((n / 2) - 1)

        // Empezamos el bucle directamente en i = 1 (saltándonos los datos huérfanos del inicio)
        for (i in 1 until n / 2) {
            val r = fft[2 * i].toFloat()
            val j = fft[2 * i + 1].toFloat()

            // Guardamos el resultado en i - 1 para rellenar nuestro array desde la posición 0
            datos[i - 1] = kotlin.math.hypot(r, j)
        }

        // Enviamos solo las frecuencias musicales puras a tu vista
        runOnUiThread {
            binding.visualizadorEQ.actualizarBarras(datos)
        }
    }


    private fun iniciarBucleProgreso() {
        detenerBucleProgreso()

        trabajoProgreso = lifecycleScope.launch {
            while (true) {
                val posActual = exoPlayer?.currentPosition ?: 0

                // Actualizamos la vbarra de rpogreso y el texto
                binding.seekBarraProgreso.progress = posActual.toInt()
                binding.txtProgreso.text = posActual.toMinSeg()

                delay(500.milliseconds)
            }
        }
    }

    private fun detenerBucleProgreso() {
        trabajoProgreso?.cancel()
        trabajoProgreso = null
    }

    private fun initPermisos() {
        // Definimos el permiso de lectura según la versión del dispositivo (Android 8 a 14+)
        val permisoMusica = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val listaPermisos = arrayOf(permisoMusica, Manifest.permission.RECORD_AUDIO)

        // Comprobamos si falta alguno de los dos permisos por aceptar
        val faltanPermisos = listaPermisos.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (faltanPermisos) {
            // Lanza la solicitud de ambos en una sola línea de código
            lanzadorPermisos.launch(listaPermisos)
        } else {
            // Si ya tiene ambos, carga la música directamente

        }
    }

    override fun onResume() {
        super.onResume()
        initPermisos()
    }

    // 5. IMPORTANTE: Liberar la memoria del teléfono al cerrar la app (Evita crasheos fatales)
    override fun onDestroy() {
        super.onDestroy()
        detenerBucleProgreso()
        exoPlayer?.release()
        exoPlayer = null
    }

}