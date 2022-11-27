package com.example.appyHighBrowser.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class DownloadModel : RealmObject() {
    @PrimaryKey
    var id: Long = 0
    var downloadId: Long = 0
    var title: String? = null
    var file_path: String? = null
}