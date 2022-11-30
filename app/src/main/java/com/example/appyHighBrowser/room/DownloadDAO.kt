package com.example.appyHighBrowser.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appyHighBrowser.model.DownloadModel

@Dao
interface DownloadDAO {

    @Query("SELECT * FROM downloadsTable")
    suspend fun getAllDownloads(): List<DownloadModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertDownload(download: DownloadModel)

}