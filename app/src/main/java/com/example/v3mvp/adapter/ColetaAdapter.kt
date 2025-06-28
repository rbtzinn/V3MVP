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
            val latitude = coleta.latitude ?: 0.0
            val longitude = coleta.longitude ?: 0.0
            val x = coleta.gyroX ?: 0.0f
            val y = coleta.gyroY ?: 0.0f
            val z = coleta.gyroZ ?: 0.0f
            val enviadoTexto = if (coleta.enviado) "✔️ Enviado" else "❌ Não enviado"

            val textoFormatado = buildString {
                appendLine("📍 Localização:")
                appendLine("  Lat: %.6f".format(latitude))
                appendLine("  Long: %.6f".format(longitude))
                appendLine("🌀 Giroscópio:")
                appendLine("  X: %.4f".format(x))
                appendLine("  Y: %.4f".format(y))
                appendLine("  Z: %.4f".format(z))
                appendLine("📦 Status: $enviadoTexto")
            }

            txtDados.text = textoFormatado
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
