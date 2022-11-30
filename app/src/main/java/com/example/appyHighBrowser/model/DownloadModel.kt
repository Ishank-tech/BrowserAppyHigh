package com.example.appyHighBrowser.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloadsTable")
class DownloadModel (
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var downloadId: Long = 0,
    var title: String? = null,
    var file_path: String? = null,
)