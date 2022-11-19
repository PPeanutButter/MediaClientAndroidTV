package com.peanut.ted.ed

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.peanut.ted.ed.Unities.gone
import com.peanut.ted.ed.Unities.httpThread
import com.peanut.ted.ed.Unities.name
import com.peanut.ted.ed.Unities.resolveUrl
import com.peanut.ted.ed.Unities.round
import com.peanut.ted.ed.Unities.visible
import com.peanut.ted.ed.databinding.ActivityDetailBinding
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.max

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var album: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        album = (intent.getStringExtra("ALBUM")?:"错误")
        binding.cover.round(20f)
        refresh()
    }

    private fun refresh() = thread { refreshOnThread() }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun refreshOnThread() {
        try {
            getJson(album) {
                val images = ArrayList<String>()
                val titles = ArrayList<String>()
                val attaches = ArrayList<String>()
                val times = ArrayList<String>()
                val server = SettingManager.getValue("ip", "").resolveUrl()
                for (index in 0 until it.length()) {
                    val jsonObject = it.getJSONObject(index)
                    val episode = jsonObject.getString("name")
                    if (jsonObject.getString("type") == "Attach")
                        attaches.add(episode)
                    else if (jsonObject.getString("watched") != "watched" || SettingManager.getValue("show_watched", false)) {
                        titles.add(episode.name())
                        images.add("$server/getVideoPreview?path=${Uri.encode(episode)}&" +
                                "token=${SettingManager.getValue("token", "")}")
                        times.add(
                            Unities.getFileLengthDesc(jsonObject.getLong("length")) + "  " + jsonObject.getString(
                                "desc"
                            )
                        )
                    }
                }
                Handler(mainLooper).post {
                    Picasso.get().load("$server/getFile/get_post_img?" +
                            "path=${Uri.encode("/$album/.post")}&" +
                            "token=${SettingManager.getValue("token", "")}").error(R.mipmap.post)
                        .placeholder(R.mipmap.post).into(binding.post)
                    Picasso.get().load("$server/getCover?cover=${Uri.encode(album)}&" +
                            "token=${SettingManager.getValue("token", "")}").error(R.mipmap.cover)
                        .placeholder(R.mipmap.cover).into(binding.cover)
                    val rv = binding.rv
                    if (attaches.size == 0){
                        binding.download.gone()
                        binding.downloadIcon.gone()
                    }else{
                        binding.download.setOnClickListener {
                            MaterialDialog(this).show {
                                title(text = "附件")
                                listItems(items = attaches){ _: MaterialDialog, _: Int, charSequence: CharSequence->
                                    try{
                                        Unities.downloadFile(
                                            path = Environment.getExternalStorageDirectory().path + "/Download",
                                            fileName = charSequence.substring(max(charSequence.lastIndexOf("/"), charSequence.lastIndexOf("%2f"))),
                                            url = "$server/getFile/${charSequence.toString().name()}?" +
                                                    "path=${Uri.encode("/$charSequence")}&" +
                                                    "token=${SettingManager.getValue("token", "")}",
                                            onOkHttpDownloaderUpdate = object : Listener.OnOkHttpDownloaderUpdate {
                                                override fun update(percentage: Int) {}

                                                override fun onDownloadFailed(message: String) {
                                                    Handler(context.mainLooper).post {
                                                        Toast.makeText(context, "下载失败:${message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }

                                                override fun onDownloadSuccessful(file: File) {
                                                    Handler(context.mainLooper).post {
                                                        Toast.makeText(context, "下载完成:${charSequence.toString().name()}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )
                                    }catch (e:Exception){
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                    if (times.size == 0){
                        rv.gone()
                    }else {
                        rv.layoutManager = StaggeredGridLayoutManager(
                            1, StaggeredGridLayoutManager.VERTICAL
                        )
                        rv.adapter = EpisodeAdapter(
                            this,
                            titles = titles,
                            images = images,
                            dates = times,
                            album = album
                        )
                    }
                }
            }
            getInfo {
                Handler(mainLooper).post {
                    binding.textView.text = it.getString("title")
                    binding.textView2.text =
                        "${it.getString("certification")} ${it.getString("genres")} • ${it.getString("runtime")}"
                    binding.textView3.text = it.getString("tagline")
                    binding.textView4.text = "剧情简介"
                    binding.textView5.text = it.getString("overview")
                    binding.include2.root.visible()
                    binding.include2.textView6.text = it.getInt("user_score_chart").toString()
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
            Handler(mainLooper).post {
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getJson(album:String, func: (JSONArray) -> Unit) {
        val server = SettingManager.getValue("ip", "192.168.10.208").resolveUrl()
        "$server/getFileList?path=/$album/".httpThread { body ->
            func.invoke(JSONArray(body ?: "[]"))
        }
    }

    private fun getInfo(func: (JSONObject) -> Unit) {
        val server = SettingManager.getValue("ip", "192.168.10.208").resolveUrl()
        "$server/getFile/get_album_info?path=${Uri.encode("/$album/.info")}".httpThread { body ->
            func.invoke(JSONObject(body ?: "{}"))
        }
    }
}