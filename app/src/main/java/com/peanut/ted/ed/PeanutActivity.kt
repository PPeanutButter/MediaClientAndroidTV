package com.peanut.ted.ed

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.peanut.ted.ed.Unities.toast

open class PeanutActivity : AppCompatActivity() {
    var onActivityResultListener = mutableListOf<Pair<Int,(Int, Intent?)->Unit>>()
    private var onRequestPermissionsResultListener = mutableListOf<Pair<Int,(Array<out String>, IntArray)->Unit>>()

    val Int.dp: Int
        get() {
            return (this * this@PeanutActivity.resources.displayMetrics.density + 0.5f).toInt()
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        for (a in onActivityResultListener){
            if (a.first == requestCode) {
                a.second.invoke(resultCode,data)
                onActivityResultListener.remove(a)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (a in onRequestPermissionsResultListener){
            if (a.first == requestCode) {
                a.second.invoke(permissions,grantResults)
                onRequestPermissionsResultListener.remove(a)
            }
        }
    }

    /**
     * requestCode：0000,0000,0000,0000,11__,____,____,____
     */
    fun fileChooser(mimeType:String = "application/*",func:(resultCode:Int, data:Intent?)->Unit){
        val requestCode: Int = (Math.random()*0xffff).toInt() or 0xc000
        onActivityResultListener.add(requestCode to func)
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE){grantResult ->
            if (grantResult){
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = mimeType
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(intent, requestCode)
            }else "存储权限被拒绝".toast(this)
        }
    }

    /**
     * requestCode：0000,0000,0000,0000,01__,____,____,____
     */
    fun requestPermissionSuccess(permission:String,func:()->Unit){
        if(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            func.invoke()
        else
            requestPermissions(arrayOf(permission)){ _, grantResults ->
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                func.invoke()
            }else "存储权限被拒绝".toast(this)
        }
    }
    fun requestPermissions(permissions:Array<String>,func:(permissions: Array<out String>, grantResults: IntArray)->Unit){
        val requestCode: Int = (Math.random()*0xffff).toInt() and 0x7fff or 0x4000
        onRequestPermissionsResultListener.add(requestCode to func)
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    fun requestPermission(permission:String,func:(grantResult: Boolean)->Unit) =
        requestPermissions(arrayOf(permission)){ _, grantResults ->
            func.invoke(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }

    /**
     * requestCode：0000,0000,0000,0000,10__,____,____,____
     */
    fun callActivity(intent: Intent,func:(resultCode:Int, data:Intent?)->Unit){
        val requestCode: Int = (Math.random()*0xffff).toInt() and 0xbfff or 0x8000
        onActivityResultListener.add(requestCode to func)
        startActivityForResult(intent, requestCode)
    }

}