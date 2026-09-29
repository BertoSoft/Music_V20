package com.example.music_v20.modulos.home.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.music_v20.databinding.ItemPlayerBinding
import com.example.music_v20.modulos.home.domain.extensions.toMinSeg
import com.example.music_v20.modulos.home.domain.model.Cancion
import javax.inject.Inject


class PlayerAdapter @Inject constructor(
    var onItemClick: ((Cancion) -> Unit)? = null
): ListAdapter<Cancion, PlayerAdapter.PlayerViewHolder>(diffCallBack){

    private var idItemSelecionado = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlayerViewHolder {
        val binding = ItemPlayerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PlayerViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        val estaSeleccionado = item.id == idItemSelecionado
        holder.render(item, estaSeleccionado)
    }

    inner class PlayerViewHolder(
        private val binding: ItemPlayerBinding
    ): RecyclerView.ViewHolder(binding.root){

        fun render(cancion: Cancion, estaSelecionado: Boolean){
            with(binding){
                txtCancion.text = cancion.titulo
                txtDuracion.text = cancion.duracion.toMinSeg()
                root.isSelected = estaSelecionado
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(cancion)
            }
        }
    }

    fun setSeleccion(id: Int) {
        // Selecciona una fila del listado
        val oldId = idItemSelecionado
        idItemSelecionado = id

        val posOld = currentList.indexOfFirst { it.id == oldId }
        val posNew = currentList.indexOfFirst { it.id == id }

        if (posOld != -1) notifyItemChanged(posOld)
        if (posNew != -1) notifyItemChanged(posNew)
    }

    companion object diffCallBack: DiffUtil.ItemCallback<Cancion>(){
        override fun areItemsTheSame(
            oldItem: Cancion,
            newItem: Cancion
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Cancion,
            newItem: Cancion
        ): Boolean {
            return oldItem == newItem
        }

    }
}