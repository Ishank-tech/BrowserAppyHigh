package com.example.appyHighBrowser.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.example.appyHighBrowser.R
import com.example.appyHighBrowser.activity.MainActivity.Companion.myPager
import com.example.appyHighBrowser.activity.MainActivity.Companion.tabsBtn
import com.example.appyHighBrowser.adapter.TabAdapter
import com.example.appyHighBrowser.databinding.ActivityMainBinding
import com.example.appyHighBrowser.databinding.BookmarkDialogBinding
import com.example.appyHighBrowser.databinding.MoreFeaturesBinding
import com.example.appyHighBrowser.databinding.TabsViewBinding
import com.example.appyHighBrowser.fragment.BrowseFragment
import com.example.appyHighBrowser.fragment.HomeFragment
import com.example.appyHighBrowser.model.Bookmark
import com.example.appyHighBrowser.model.Tab
import java.io.ByteArrayOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    companion object {
        var tabsList: ArrayList<Tab> = ArrayList()
        var bookmarkList: ArrayList<Bookmark> = ArrayList()
        var bookmarkIndex: Int = -1
        lateinit var myPager: ViewPager2
        lateinit var tabsBtn: MaterialTextView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getAllBookmarks()
        setUpTabs()
        initializeView()
    }

    private fun setUpTabs() {
        tabsList.add(Tab("Home", HomeFragment()))
        binding.myPager.adapter = TabsAdapter(supportFragmentManager, lifecycle)
        binding.myPager.isUserInputEnabled = false
        myPager = binding.myPager
        tabsBtn = binding.tabsBtn
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (e: Exception) {
        }

        when {
            frag?.binding?.webView?.canGoBack() == true -> frag.binding.webView.goBack()
            binding.myPager.currentItem != 0 -> {
                tabsList.removeAt(binding.myPager.currentItem)
                binding.myPager.adapter?.notifyDataSetChanged()
                binding.myPager.currentItem = tabsList.size - 1

            }
            else -> super.onBackPressed()
        }
    }


    private inner class TabsAdapter(fa: FragmentManager, lc: Lifecycle) :
        FragmentStateAdapter(fa, lc) {
        override fun getItemCount(): Int = tabsList.size

        override fun createFragment(position: Int): Fragment = tabsList[position].fragment
    }


    private fun initializeView() {

        binding.tabsBtn.setOnClickListener {
            val viewTabs = layoutInflater.inflate(R.layout.tabs_view, binding.root, false)
            val bindingTabs = TabsViewBinding.bind(viewTabs)

            val dialogTabs =
                MaterialAlertDialogBuilder(this, R.style.roundCornerDialog).setView(viewTabs)
                    .setTitle("Select Tab")
                    .setPositiveButton("Home") { self, _ ->
                        changeTab("Home", HomeFragment())
                        self.dismiss()
                    }
                    .setNeutralButton("Google") { self, _ ->
                        changeTab("Google", BrowseFragment(urlNew = "www.google.com"))
                        self.dismiss()
                    }
                    .create()

            bindingTabs.tabsRV.setHasFixedSize(true)
            bindingTabs.tabsRV.layoutManager = LinearLayoutManager(this)
            bindingTabs.tabsRV.adapter = TabAdapter(this, dialogTabs)

            dialogTabs.show()

            val pBtn = dialogTabs.getButton(AlertDialog.BUTTON_POSITIVE)
            val nBtn = dialogTabs.getButton(AlertDialog.BUTTON_NEUTRAL)

            pBtn.isAllCaps = false
            nBtn.isAllCaps = false

            pBtn.setTextColor(Color.BLACK)
            nBtn.setTextColor(Color.BLACK)

            pBtn.setCompoundDrawablesWithIntrinsicBounds(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_home, theme), null, null, null
            )
            nBtn.setCompoundDrawablesWithIntrinsicBounds(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_add, theme), null, null, null
            )
        }

        binding.settingBtn.setOnClickListener {

            var frag: BrowseFragment? = null
            try {
                frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
            } catch (e: Exception) {
            }

            val view = layoutInflater.inflate(R.layout.more_features, binding.root, false)
            val dialogBinding = MoreFeaturesBinding.bind(view)

            val dialog = MaterialAlertDialogBuilder(this).setView(view).create()

            dialog.window?.apply {
                attributes.gravity = Gravity.BOTTOM
                attributes.y = 50
                setBackgroundDrawable(ColorDrawable(0xFFFFFFFF.toInt()))
            }
            dialog.show()

            frag?.let {
                bookmarkIndex = isBookmarked(it.binding.webView.url!!)
                if (bookmarkIndex != -1) {

                    dialogBinding.bookmarkBtn.apply {
                        setIconTintResource(R.color.cool_blue)
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
                    }
                }
            }



            dialogBinding.backBtn.setOnClickListener {
                onBackPressed()
            }

            dialogBinding.downloadsButton.setOnClickListener {
                val intent = Intent(applicationContext, DownloadActivity::class.java)
                startActivity(intent)
            }


            dialogBinding.bookmarkBtn.setOnClickListener {
                frag?.let {
                    if (bookmarkIndex == -1) {
                        val viewB =
                            layoutInflater.inflate(R.layout.bookmark_dialog, binding.root, false)
                        val bBinding = BookmarkDialogBinding.bind(viewB)
                        val dialogB = MaterialAlertDialogBuilder(this)
                            .setTitle("Add Bookmark")
                            .setMessage("Url:${it.binding.webView.url}")
                            .setPositiveButton("Add") { self, _ ->
                                try {
                                    val array = ByteArrayOutputStream()
                                    it.webIcon?.compress(Bitmap.CompressFormat.PNG, 100, array)
                                    bookmarkList.add(
                                        Bookmark(
                                            name = bBinding.bookmarkTitle.text.toString(),
                                            url = it.binding.webView.url!!,
                                            array.toByteArray()
                                        )
                                    )
                                } catch (e: Exception) {
                                    bookmarkList.add(
                                        Bookmark(
                                            name = bBinding.bookmarkTitle.text.toString(),
                                            url = it.binding.webView.url!!
                                        )
                                    )
                                }
                                self.dismiss()
                            }
                            .setNegativeButton("Cancel") { self, _ -> self.dismiss() }
                            .setView(viewB).create()
                        dialogB.show()
                        bBinding.bookmarkTitle.setText(it.binding.webView.title)
                    } else {
                        val dialogB = MaterialAlertDialogBuilder(this)
                            .setTitle("Remove Bookmark")
                            .setMessage("Url:${it.binding.webView.url}")
                            .setPositiveButton("Remove") { self, _ ->
                                bookmarkList.removeAt(bookmarkIndex)
                                self.dismiss()
                            }
                            .setNegativeButton("Cancel") { self, _ -> self.dismiss() }
                            .create()
                        dialogB.show()
                    }
                }

                dialog.dismiss()
            }
        }

    }

    fun isBookmarked(url: String): Int {
        bookmarkList.forEachIndexed { index, bookmark ->
            if (bookmark.url == url) return index
        }
        return -1
    }

    fun saveBookmarks() {
        //for storing bookmarks data using shared preferences
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()

        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)

        editor.apply()
    }

    private fun getAllBookmarks() {
        //for getting bookmarks data using shared preferences from storage
        bookmarkList = ArrayList()
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE)
        val data = editor.getString("bookmarkList", null)

        if (data != null) {
            val list: ArrayList<Bookmark> = GsonBuilder().create()
                .fromJson(data, object : TypeToken<ArrayList<Bookmark>>() {}.type)
            bookmarkList.addAll(list)
        }
    }

}

@SuppressLint("NotifyDataSetChanged")
fun changeTab(url: String, fragment: Fragment, isBackground: Boolean = false) {
    MainActivity.tabsList.add(Tab(name = url, fragment = fragment))
    myPager.adapter?.notifyDataSetChanged()
    tabsBtn.text = MainActivity.tabsList.size.toString()

    if (!isBackground) myPager.currentItem = MainActivity.tabsList.size - 1
}

fun checkForInternet(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION") val networkInfo =
            connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        return networkInfo.isConnected
    }
}