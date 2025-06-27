package com.example.v3mvp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.v3mvp.R
import com.example.v3mvp.model.Coleta

class ColetaAdapter : ListAdapter<Coleta, ColetaAdapter.ColetaViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Coleta>() {
            override fun areItemsTheSame(oldItem: Coleta, newItem: Coleta): Boolean {
                return oldItem.timestamp == newItem.timestamp
            }

            override fun areContentsTheSame(oldItem: Coleta, newItem: Coleta): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class ColetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDados: TextView = itemView.findViewById(R.id.txtDados)

        fun bind(coleta: Coleta) {
            val texto = "Lat: ${coleta.latitude}, Long: ${coleta.longitude}, Gyro: ${coleta.gyroX}, ${coleta.gyroY}, ${coleta.gyroZ}"
            txtDados.text = texto
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColetaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coleta, parent, false)
        return ColetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColetaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
