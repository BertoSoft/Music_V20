package com.example.music_v20.modulos.home.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.music_v20.R
import com.example.music_v20.databinding.ActivityPlayerBinding
import com.example.music_v20.modulos.home.ui.viewmodel.PlayerUiEstado
import com.example.music_v20.modulos.home.ui.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private val solicitarPermisosAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ _ -> }
    lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUi()

    }

    private fun initUi() {
        initRv()
        initListeners()
        initObservers()

    }

    private fun initRv() {

    }

    private fun initListeners() {

    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.estado.collect { estado ->
                    gestionaPlayer(estado)
                    dibujaUi(estado)
                }
            }
        }
    }

    fun dibujaUi(estado: PlayerUiEstado) {
        with(binding){

            // 0.- Barra de Progreso
            if(estado.isCargando){
                pbCargando.visibility = View.VISIBLE
                rvCanciones.alpha = 0.5F
            }
            else{
                pbCargando.visibility = View.GONE
                rvCanciones.alpha = 1.0F
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

    fun gestionaPlayer(estado: PlayerUiEstado) {

    }

    private fun initPermisos() {
        if(androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO) != 0
        ){
            solicitarPermisosAudio.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onResume() {
        super.onResume()
        initPermisos()
    }



}