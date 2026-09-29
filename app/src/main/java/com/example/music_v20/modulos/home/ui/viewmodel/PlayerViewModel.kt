package com.example.music_v20.modulos.home.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_v20.modulos.home.domain.model.Cancion
import com.example.music_v20.modulos.home.domain.usecase.PlayerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExoPlayerEstado{Play, Pause, Stop}

data class PlayerUiEstado(
    val cancionActual: Cancion? = null,
    val listaCanciones: List<Cancion>? = null,
    val isCargando: Boolean = false,
    val msgError: String? = null
)

data class ExoPlayerUiEstado(
    val cancionActual: Cancion? = null,
    val estadoPlayer: ExoPlayerEstado = ExoPlayerEstado.Stop
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val useCase: PlayerUseCase
): ViewModel() {

    private val _estado = MutableStateFlow<PlayerUiEstado>(PlayerUiEstado())
    val estado: StateFlow<PlayerUiEstado> get() = _estado

    private val _player = MutableStateFlow<ExoPlayerUiEstado>(ExoPlayerUiEstado())
    val player: StateFlow<ExoPlayerUiEstado> get() = _player

    init {
        viewModelScope.launch {
            try {
                _estado.update { estado ->
                    estado.copy( isCargando = true)
                }
                val listaCanciones = useCase.getAllCancionesUseCase()
                if(listaCanciones.isNotEmpty()){
                    _estado.update { estado ->
                        estado.copy(
                            isCargando = false,
                            listaCanciones = listaCanciones,
                            cancionActual = listaCanciones[0],
                            msgError = null
                        )
                    }
                }
                else{
                    _estado.update { estado ->
                        estado.copy(
                            isCargando = false,
                            listaCanciones = null,
                            cancionActual = null,
                            msgError = "Lista Canciones Vacia"
                        )
                    }
                }
            }
            catch (e: Exception){
                _estado.update { estado ->
                    estado.copy(
                        isCargando = false,
                        listaCanciones = null,
                        cancionActual = null,
                        msgError = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    // Funciones Publicas
    fun setCancionActual(cancion: Cancion){
        _estado.update { estado ->
            estado.copy(
                cancionActual = cancion
            )
        }

        _player.update { estado ->
            estado.copy(
                cancionActual = cancion
            )
        }
    }

    fun playClick(){
        when(_player.value.estadoPlayer){
            ExoPlayerEstado.Stop ->{
                if(_player.value.cancionActual == null){
                    _player.update { estado ->
                        estado.copy(
                            cancionActual =  _estado.value.cancionActual,
                            estadoPlayer = ExoPlayerEstado.Play
                        )
                    }
                }
                else{
                    _player.update { estado ->
                        estado.copy(
                            estadoPlayer = ExoPlayerEstado.Play
                        )
                    }
                }
            }
            ExoPlayerEstado.Play ->{
                _player.update { estado ->
                    estado.copy(
                        estadoPlayer = ExoPlayerEstado.Pause
                    )
                }
            }
            ExoPlayerEstado.Pause ->{
                _player.update { estado ->
                    estado.copy(
                        estadoPlayer = ExoPlayerEstado.Play
                    )
                }
            }
        }
    }

    fun atrasClick(){
        val lista = _estado.value.listaCanciones
        val actual = _estado.value.cancionActual

        if(lista != null && actual != null){
            var id = actual.id - 1
            if(id < 0){
                id = lista.size - 1
            }
            val cancion = lista.find { it.id == id } ?: actual
            setCancionActual(cancion)
        }
    }

    fun adelanteClick(){
        val lista = _estado.value.listaCanciones
        val actual = _estado.value.cancionActual

        if(lista != null && actual != null){
            var id = actual.id + 1
            if(id > (lista.size - 1)){
                id = 0
            }
            val cancion = lista.find { it.id == id } ?: actual
            setCancionActual(cancion)
        }
    }

}