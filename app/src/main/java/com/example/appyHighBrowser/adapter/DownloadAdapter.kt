package com.example.appyHighBrowser.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appyHighBrowser.R
import com.example.appyHighBrowser.Utils.ClickListener
import com.example.appyHighBrowser.model.DownloadModel

class DownloadAdapter(
    downloadModels: List<DownloadModel>,
    var clickListener: ClickListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var downloadModels: List<DownloadModel> = ArrayList()

    init {
        this.downloadModels = downloadModels
    }

    inner class DownloadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var file_title: TextView
        var main_rel: RelativeLayout

        init {
            file_title = itemView.findViewById(R.id.file_title)
            main_rel = itemView.findViewById(R.id.main_rel)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val vh: RecyclerView.ViewHolder
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.download_row, parent, false)
        vh = DownloadViewHolder(view)
        return vh
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val downloadModel = downloadModels[position]
        val downloadViewHolder = holder as DownloadViewHolder
        downloadViewHolder.file_title.text = downloadModel.title
        downloadViewHolder.main_rel.setOnClickListener { clickListener.onCLickItem(downloadModel.file_path) }
    }

    override fun getItemCount(): Int {
        return downloadModels.size
    }
}