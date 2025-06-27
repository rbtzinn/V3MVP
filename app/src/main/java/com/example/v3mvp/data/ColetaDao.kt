package com.example.v3mvp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.v3mvp.model.Coleta

@Dao
interface ColetaDao {
    @Insert
    suspend fun inserir(coleta: Coleta)

    @Query("SELECT * FROM coleta ORDER BY timestamp DESC")
    suspend fun buscarTodas(): List<Coleta>

    @Query("DELETE FROM coleta")
    suspend fun deletarTodas()

}

