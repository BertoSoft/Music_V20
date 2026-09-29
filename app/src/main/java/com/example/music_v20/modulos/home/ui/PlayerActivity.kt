package com.example.music_v20.modulos.home.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
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

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()
    private  val miAdaptador: PlayerAdapter by lazy { PlayerAdapter() }
    private var exoPlayer: ExoPlayer? = null
    private var trabajoProgreso: Job? = null

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
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    private fun initRv() {
        with(binding.rvCanciones){
            layoutManager = LinearLayoutManager(this@PlayerActivity)
            adapter = miAdaptador
            setHasFixedSize(true)
        }

        miAdaptador.onItemClick = { cancion ->
            viewModel.setCancionActual(cancion)
        }
    }

    private fun initListeners() {
        with(binding){
            btnPlay.setOnClickListener {
                viewModel.playClick()
            }
            btnAdelante.setOnClickListener {
                viewModel.adelanteClick()
            }
            btnAtras.setOnClickListener {
                viewModel.atrasClick()
            }
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.estado.collect { estadoUi ->
                    dibujaUi(estadoUi)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.player.collect { estadoPlayer ->
                    gestionaPlayer(estadoPlayer)
                }
            }
        }
    }

    fun dibujaUi(estado: PlayerUiEstado) {
        with(binding){

            // 0.- Barra de Progreso, se activa cuando isCargando es cierto
            if(estado.isCargando){
                pbCargando.visibility = View.VISIBLE
                rvCanciones.alpha = 0.5F
            }
            else{
                pbCargando.visibility = View.GONE
                rvCanciones.alpha = 1.0F
            }

            // 1.- RecyclerView entra cuando la lista del adaptador != estado.lista
            if(miAdaptador.currentList != estado.listaCanciones){
                miAdaptador.submitList(estado.listaCanciones)
            }

            // 2.- Cancion Actual y duracion, cuando existe cancionActual y titulo != txtCancion.text
            if(estado.cancionActual != null && estado.cancionActual.titulo != txtCancion.text){
                txtCancion.text = estado.cancionActual.titulo
                txtCancion.tag = txtCancion.id
                txtDuracion.text = estado.cancionActual.duracion.toMinSeg()
                seekBarraProgreso.max = estado.cancionActual.duracion.toInt()
                miAdaptador.setSeleccion(estado.cancionActual.id)
                seekBarraProgreso.progress = 0
                txtProgreso.text = "00:00"
            }

            // 5.- Mensajes de error
            if(estado.msgError != null){
                Toast.makeText(
                    this@PlayerActivity,
                    estado.msgError,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun gestionaPlayer(estado: ExoPlayerUiEstado) {
        if(estado.cancionActual == null) return

        when(estado.estadoPlayer){

            //exoPlayer()
            ExoPlayerEstado.Play -> {
                val rutaNueva = estado.cancionActual.ruta ?: ""

                binding.btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                binding.visualizadorEQ.visibility = View.VISIBLE

                if(exoPlayer?.currentMediaItem == null ||
                    exoPlayer?.currentMediaItem?.mediaId != estado.cancionActual.id_android.toString()){

                    val mediaItem = MediaItem.Builder()
                        .setUri(rutaNueva)
                        .setMediaId(estado.cancionActual.id_android.toString()) // Guardamos el ID para identificarla
                        .build()

                    exoPlayer?.setMediaItem(mediaItem)
                    exoPlayer?.prepare()
                }
                exoPlayer?.play()
                iniciarBucleProgreso();
            }
            ExoPlayerEstado.Pause -> {
                // Sincronización de UI: Botón central cambia a PLAY y pausamos ecualizador
                binding.btnPlay.setImageResource(android.R.drawable.ic_media_play)
                binding.visualizadorEQ.visibility = View.INVISIBLE

                // Lógica Física: Pausamos ExoPlayer y congelamos el hilo de la barra
                exoPlayer?.pause()
                detenerBucleProgreso()
            }

            ExoPlayerEstado.Stop -> {
                binding.btnPlay.setImageResource(android.R.drawable.ic_media_play)
                binding.visualizadorEQ.visibility = View.INVISIBLE

                exoPlayer?.stop()
                detenerBucleProgreso()

                // Reseteamos valores visuales a cero
                binding.seekBarraProgreso.progress = 0
                binding.txtProgreso.text = "00:00"
            }
        }
    }

    private fun iniciarBucleProgreso(){
        detenerBucleProgreso()

        trabajoProgreso = lifecycleScope.launch {
            while (true){
                val posActual = exoPlayer?.currentPosition ?: 0

                // Actualizamos la vbarra de rpogreso y el texto
                binding.seekBarraProgreso.progress = posActual.toInt()
                binding.txtProgreso.text = posActual.toMinSeg()

                delay(500.milliseconds)
            }
        }
    }

    private fun detenerBucleProgreso(){
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