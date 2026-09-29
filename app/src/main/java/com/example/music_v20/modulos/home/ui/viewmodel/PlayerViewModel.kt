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


data class PlayerUiEstado(
    val cancionActual: Cancion? = null,
    val listaCanciones: List<Cancion>? = null,
    val isCargando: Boolean = false,
    val msgError: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val useCase: PlayerUseCase
): ViewModel() {

    private val _estado = MutableStateFlow<PlayerUiEstado>(PlayerUiEstado())
    val estado: StateFlow<PlayerUiEstado> get() = _estado

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


}