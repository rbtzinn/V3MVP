package com.example.v3mvp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.v3mvp.service.ColetaService
import com.example.v3mvp.util.Exportador
import com.example.v3mvp.viewmodel.ColetaViewModel
import android.widget.Button
import androidx.core.content.ContextCompat.startForegroundService
import com.example.v3mvp.adapter.ColetaAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ColetaViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnExportar: Button
    private lateinit var btnLimpar: Button
    private lateinit var adapter: ColetaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ColetaViewModel(application)
        recyclerView = findViewById(R.id.recyclerColetas)
        btnExportar = findViewById(R.id.btnExportar)
        btnLimpar = findViewById(R.id.btnLimpar)

        adapter = ColetaAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnExportar.setOnClickListener {
            Exportador.exportarColetas(this, viewModel.coletas.value ?: emptyList())

        }

        btnLimpar.setOnClickListener {
            viewModel.limparColetas()
        }

        viewModel.coletas.observe(this) {
            adapter.submitList(it)
        }

        checarPermissoes()
        viewModel.inserirColetasTeste()
    }

    private fun checarPermissoes() {
        val permissoes = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )

        if (permissoes.all {
                ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            iniciarServico()
        } else {
            ActivityCompat.requestPermissions(this, permissoes, 100)
        }
    }


    private fun iniciarServico() {
        val intent = Intent(this, ColetaService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            iniciarServico()
        }
    }
}
