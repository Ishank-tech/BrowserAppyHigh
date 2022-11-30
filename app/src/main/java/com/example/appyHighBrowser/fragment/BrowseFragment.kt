package com.example.appyHighBrowser.fragment

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.SpannableStringBuilder
import android.view.*
import android.webkit.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.appyHighBrowser.R
import com.example.appyHighBrowser.activity.MainActivity
import com.example.appyHighBrowser.databinding.FragmentBrowseBinding
import com.example.appyHighBrowser.activity.DownloadActivity
import com.example.appyHighBrowser.model.DownloadModel
import com.example.appyHighBrowser.room.DownloadDAO
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class BrowseFragment(private var urlNew: String) : Fragment() {

    lateinit var binding: FragmentBrowseBinding
    var webIcon: Bitmap? = null
    val downloadModel = DownloadModel()

    @Inject
    lateinit var downloadDao : DownloadDAO

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_browse, container, false)
        binding = FragmentBrowseBinding.bind(view)
        binding.webView.apply {
            when {
                URLUtil.isValidUrl(urlNew) -> loadUrl(urlNew)
                urlNew.contains(".com", ignoreCase = true) -> loadUrl(urlNew)
                else -> loadUrl("https://www.google.com/search?q=$urlNew")
            }
        }

        return view
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        MainActivity.tabsList[MainActivity.myPager.currentItem].name =
            binding.webView.url.toString()
        MainActivity.tabsBtn.text = MainActivity.tabsList.size.toString()
        setUpWebviewDownloadListener()
        val mainRef = requireActivity() as MainActivity

        mainRef.binding.refreshBtn.visibility = View.VISIBLE
        mainRef.binding.refreshBtn.setOnClickListener {
            binding.webView.reload()
        }

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    mainRef.binding.topSearchBar.text = SpannableStringBuilder(url)
                    MainActivity.tabsList[MainActivity.myPager.currentItem].name = url.toString()
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    mainRef.binding.progressBar.progress = 0
                    mainRef.binding.progressBar.visibility = View.VISIBLE
                    if (url!!.contains(
                            "you",
                            ignoreCase = false
                        )
                    ) mainRef.binding.root.transitionToEnd()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    mainRef.binding.progressBar.visibility = View.GONE
                    binding.webView.zoomOut()
                }
            }
            webChromeClient = object : WebChromeClient() {
                //for setting icon to our search bar
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    try {
                        mainRef.binding.webIcon.setImageBitmap(icon)
                        webIcon = icon
                        MainActivity.bookmarkIndex = mainRef.isBookmarked(view?.url!!)
                        if (MainActivity.bookmarkIndex != -1) {
                            val array = ByteArrayOutputStream()
                            icon!!.compress(Bitmap.CompressFormat.PNG, 100, array)
                            MainActivity.bookmarkList[MainActivity.bookmarkIndex].image =
                                array.toByteArray()
                        }
                    } catch (e: Exception) {
                    }
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    binding.webView.visibility = View.GONE
                    binding.customView.visibility = View.VISIBLE
                    binding.customView.addView(view)
                    mainRef.binding.root.transitionToEnd()
                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                    binding.webView.visibility = View.VISIBLE
                    binding.customView.visibility = View.GONE

                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    mainRef.binding.progressBar.progress = newProgress
                }
            }

            binding.webView.setOnTouchListener { _, motionEvent ->
                mainRef.binding.root.onTouchEvent(motionEvent)
                return@setOnTouchListener false
            }

            binding.webView.reload()
        }


    }

    private fun setUpWebviewDownloadListener() {
        // handle download file
        binding.webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val request = DownloadManager.Request(
                Uri.parse(url)
            )

            request.setMimeType(mimeType)
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading File...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                    url, contentDisposition, mimeType
                )
            )
            val dm =
                requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
            val downloadID = dm!!.enqueue(request)
            Toast.makeText(context, "Downloading File", Toast.LENGTH_LONG).show()
            saveFileToDataBase(downloadID, url, contentDisposition, mimeType)
            requireActivity().registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )

        })
    }

    fun setChangeItemFilePath(path: String?) {
        downloadModel.file_path = path
        downloadDao.upsertDownload(downloadModel)
        val intent = Intent(requireActivity(), DownloadActivity::class.java)
        startActivity(intent)
        Toast.makeText(
            getContext(),
            "Downloading Complete",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveFileToDataBase(
        downloadID: Long,
        url: String,
        contentDisposition: String,
        mimeType: String
    ) {
        GlobalScope.launch {
            val currentnum: Number = downloadDao.getAllDownloads().size
            val nextId = currentnum.toInt() + 1
            downloadModel.id = nextId
            downloadModel.title =
                URLUtil.guessFileName(url, contentDisposition, mimeType)
            downloadModel.downloadId = downloadID
            downloadModel.file_path = ""
            downloadDao.upsertDownload(downloadModel)
        }
    }

    var onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("Range")
        override fun onReceive(context: Context, intent: Intent) {
            val dm =
                requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            val query = DownloadManager.Query()
            query.setFilterById(id)
            val cursor =
                dm?.query(DownloadManager.Query().setFilterById(id))
            cursor?.moveToFirst()
            val downloaded_path =
                cursor?.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            setChangeItemFilePath(downloaded_path)
        }
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as MainActivity).saveBookmarks()
        //for clearing all webview data
        binding.webView.apply {
            clearMatches()
            clearHistory()
            clearFormData()
            clearSslPreferences()
            clearCache(true)

            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        }

    }
}