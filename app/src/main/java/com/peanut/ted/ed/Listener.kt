package com.peanut.ted.ed

import java.io.File

object Listener {

    interface OnOkHttpDownloaderUpdate {
        fun update(percentage: Int)
        fun onDownloadFailed(message: String)
        fun onDownloadSuccessful(file:File)
    }
}
