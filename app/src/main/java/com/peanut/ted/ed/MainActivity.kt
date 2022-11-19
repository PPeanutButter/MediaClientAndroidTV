package com.peanut.ted.ed

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.peanut.ted.ed.Unities.httpThread
import com.peanut.ted.ed.Unities.resolveUrl
import com.peanut.ted.ed.databinding.ActivityMainBinding
import org.json.JSONArray
import java.net.URLEncoder
import kotlin.concurrent.thread

class MainActivity : PeanutActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(ActivityMainBinding.inflate(layoutInflater).also { binding = it }.root)
        supportActionBar?.elevation= 0F
        SettingManager.init(this)
        requestPermissionSuccess(Manifest.permission.WRITE_EXTERNAL_STORAGE){
            refresh()
        }
    }

    private fun refresh() = thread { refreshOnThread() }

    private fun refreshOnThread() {
        val albums = mutableListOf<String>()
        val scores = mutableListOf<Int>()
        try {
            val user = SettingManager.getValue("user", "")
            val ps = SettingManager.getValue("password", "")
            val server = SettingManager.getValue("ip", "192.168.10.208").resolveUrl()
            "$server/userLogin?name=${Uri.encode(user)}&psw=${Uri.encode(ps)}".httpThread{}
            getJson {
                for (i in 0 until it.length()) {
                    val data = it.getJSONObject(i)
                    if (data.getString("type") == "Directory")
                        albums.add(data.getString("name"))
                        scores.add(data.getInt("score"))
                }
                Handler(mainLooper).post {
                    binding.rv.adapter = AlbumAdapter(
                        this,
                        albums = albums,
                        scores = scores
                    )
                    binding.rv.layoutManager = StaggeredGridLayoutManager(
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 5,
                        StaggeredGridLayoutManager.VERTICAL
                    )
                }
            }
        } catch (e: Exception) {
            Handler(mainLooper).post {
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getJson(func: (JSONArray) -> Unit) {
        val server = SettingManager.getValue("ip", "192.168.10.208").resolveUrl()
        "$server/getFileList?path=/".httpThread{ body ->
            func.invoke(JSONArray(body ?: "[]"))
        }
    }
}