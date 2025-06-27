package com.example.v3mvp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.v3mvp.adapter.ColetaAdapter
import com.example.v3mvp.service.ColetaService
import com.example.v3mvp.util.Exportador
import com.example.v3mvp.viewmodel.ColetaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ColetaViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnExportar: Button
    private lateinit var btnLimpar: Button
    private lateinit var btnColetarAgora: Button
    private lateinit var adapter: ColetaAdapter

    private var fotoPath: String? = null
    private val REQUEST_FOTO = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicia componentes
        viewModel = ColetaViewModel(application)
        recyclerView = findViewById(R.id.recyclerColetas)
        btnExportar = findViewById(R.id.btnExportar)
        btnLimpar = findViewById(R.id.btnLimpar)
        btnColetarAgora = findViewById(R.id.btnColetarAgora)

        adapter = ColetaAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observa o LiveData das coletas (reativo)
        viewModel.coletas.observe(this) {
            adapter.submitList(it)
        }

        // Botões
        btnExportar.setOnClickListener {
            Exportador.exportarColetas(this, viewModel.coletas.value ?: emptyList())
        }
        btnLimpar.setOnClickListener {
            viewModel.limparColetas()
        }
        btnColetarAgora.setOnClickListener {
            if (!isGpsAtivo()) {
                Toast.makeText(this, "Ative o GPS!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            abrirCameraParaFoto()
        }

        checarPermissoes()
    }

    // ----- Abrir câmera, receber foto, enviar pro Service -----

    private fun abrirCameraParaFoto() {
        val nomeArquivo = "FOTO_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        val arquivo = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), nomeArquivo)
        fotoPath = arquivo.absolutePath

        val fotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            arquivo
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
        startActivityForResult(intent, REQUEST_FOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FOTO && resultCode == RESULT_OK && fotoPath != null) {
            coletarAgoraComFoto(fotoPath!!)
        }
    }

    private fun coletarAgoraComFoto(fotoPath: String) {
        val intent = Intent(this, ColetaService::class.java)
        intent.action = ColetaService.ACTION_COLETAR_AGORA
        intent.putExtra("fotoPath", fotoPath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // ------------- Permissões e utilidades ---------------

    private fun checarPermissoes() {
        val permissoes = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissoes.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (permissoes.all {
                ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            iniciarServico()
        } else {
            ActivityCompat.requestPermissions(this, permissoes.toTypedArray(), 100)
        }
    }

    private fun isGpsAtivo(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
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
