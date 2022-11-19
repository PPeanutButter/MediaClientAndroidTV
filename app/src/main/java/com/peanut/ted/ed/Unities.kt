package com.peanut.ted.ed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.os.Handler
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.Toast
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.concurrent.thread

object Unities {
    fun String.copy(context: Context) {
        val clipboard: ClipboardManager? =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("zhi", this)
        clipboard?.setPrimaryClip(clip)
    }

    fun String.name() = this.substring(this.lastIndexOf("/") + 1)

    fun ImageView.round(radius:Float){
        val roundRectangle: ViewOutlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        this.outlineProvider = roundRectangle
        this.clipToOutline = true
    }

    fun String.toast(context: Context, duration: Int = Toast.LENGTH_SHORT, delayMillis: Long = 0) =
        Handler(context.mainLooper).postDelayed({
            Toast.makeText(
                context, this,
                duration
            ).show()
        }, delayMillis)


    fun String.play(context: Context) {
        try {
            println(this)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(this), "video/mp4")
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "打开播放器失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun String.resolveUrl() = if (this.startsWith("http") || this.startsWith("HTTP")) {
        this
    } else "http://$this"

    fun String.http(context: Context, func: (String?) -> Unit) {
        thread {
            val client = getHttpClient()
            val request: Request = Request.Builder()
                .url(this)
                .build()
            client.newCall(request).execute().use { response ->
                val s = response.body?.string()
                Handler(context.mainLooper).post {
                    func.invoke(s)
                }
            }
        }
    }

    fun String.httpThread(func: (String?) -> Unit) {
        val client = getHttpClient()
        val request: Request = Request.Builder()
            .url(this)
            .build()
        client.newCall(request).execute().use { response ->
            func.invoke(response.body?.string().also { println(it) })
        }
    }

    private fun getHttpClient():OkHttpClient{
        return OkHttpClient.Builder()
            .cookieJar(object :CookieJar{
                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return if(SettingManager.getValue("token", "")!="") listOf(
                        Cookie.Builder().name("token")
                            .value(SettingManager.getValue("token", ""))
                            .domain(SettingManager.getValue("token_domain", ""))
                            .build()
                    ) else emptyList()
                }

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    for (cookie in cookies) {
                        if (cookie.name == "token")
                            SettingManager["token"] = cookie.value
                        SettingManager["token_domain"] = cookie.domain
                    }
                }
            })
            .build()
    }

    fun downloadFile(
        path: String,
        url: String,
        fileName: String,
        onOkHttpDownloaderUpdate: Listener.OnOkHttpDownloaderUpdate
    ) {
        val okHttpClient = OkHttpClient()
        val request: Request = Request.Builder()
            .url(url.also {
                println(url)
                println(fileName)
            })
            .addHeader("Connection", "close")
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                var inputStream: InputStream? = null
                val buf = ByteArray(2048)
                var len: Int
                var fos: FileOutputStream? = null
                // 创建储存下载文件的目录
                File(path).mkdirs()
                try {
                    inputStream = response.body!!.byteStream()
                    val total = response.body!!.contentLength()
                    val file = File("$path/$fileName")
                    file.delete()
                    fos = FileOutputStream(file)
                    var sum: Long = 0
                    while (inputStream.read(buf).also { len = it } != -1) {
                        fos.write(buf, 0, len)
                        sum += len.toLong()
                        onOkHttpDownloaderUpdate.update((sum * 1.0f / total * 100).toInt())
                    }
                    fos.flush()
                    onOkHttpDownloaderUpdate.onDownloadSuccessful(file)
                } catch (e: java.lang.Exception) {
                    onOkHttpDownloaderUpdate.onDownloadFailed(e.localizedMessage ?: "Unknown")
                } finally {
                    try {
                        inputStream?.close()
                        fos?.close()
                        response.body?.close()
                        response.close()
                    } catch (e: IOException) {
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onOkHttpDownloaderUpdate.onDownloadFailed(e.localizedMessage ?: "Unknown")
            }

        })
    }

    fun getFileLengthDesc(length: Long): String {
        return when {
            length.shr(30) >= 1.0 -> String.format("%.2f", length / 1024.0 / 1024.0 / 1024.0) + "GB"
            length.shr(20) >= 1.0 -> String.format("%.2f", length / 1024.0 / 1024.0) + "MB"
            length.shr(10) >= 1.0 -> String.format("%.2f", length / 1024.0) + "KB"
            else -> String.format("%.2f", length / 1.0) + "B"
        }
    }

    fun View.gone() {
        this.visibility = View.GONE
    }

    fun View.visible() {
        this.visibility = View.VISIBLE
    }

}
