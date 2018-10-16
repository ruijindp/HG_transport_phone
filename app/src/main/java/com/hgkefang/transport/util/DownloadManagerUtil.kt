package com.hgkefang.transport.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Message
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Create by admin on 2018/9/17
 * 下载管理器
 */
class DownloadManagerUtil(private val context: Context) {

    private var scheduledExecutorService: ScheduledExecutorService? = null
    private var onProgressListener: OnProgressListener? = null
    private var downloadId: Long = 0L
    private var downloadObserver: DownloadChangeObserver? = null
    private val HANDLE_DOWNLOAD = 0x001

    fun download(url: String, title: String, desc: String): Long {
        downloadObserver = DownloadChangeObserver()
        registerContentObserver()
        val uri = Uri.parse(url)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(uri)
//        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)//默认wifi和4G都可以
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setAllowedOverRoaming(false)
        request.setVisibleInDownloadsUi(true)
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, title)
        request.setTitle(title)
        request.setDescription(desc)
        request.setMimeType("application/vnd.android.package-archive")
        downloadId = downloadManager.enqueue(request)
        return downloadId
    }

    private fun registerContentObserver() {
        if (downloadObserver != null) {
            context.contentResolver.registerContentObserver(Uri.parse("content://downloads/my_downloads"), false, downloadObserver)
        }
    }

    fun unregisterContentObserver() {
        if (downloadObserver != null) {
            context.contentResolver.unregisterContentObserver(downloadObserver)
        }
    }

    /**
     * 下载前先移除前一个任务，防止重复下载
     */
    fun clearCurrentTask(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
    }

    /**
     * 监听下载进度
     */
    private inner class DownloadChangeObserver : ContentObserver(downLoadHandler) {

        init {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        }

        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         */
        override fun onChange(selfChange: Boolean) {
            scheduledExecutorService!!.scheduleAtFixedRate(progressRunnable, 0, 2, TimeUnit.SECONDS)
        }
    }

    /**
     * 关闭定时器，线程等操作
     */
    fun close() {
        if (!scheduledExecutorService!!.isShutdown) {
            scheduledExecutorService!!.shutdown()
        }
        if (downLoadHandler != null) {
            downLoadHandler!!.removeCallbacksAndMessages(null)
        }
    }

    private val progressRunnable = Runnable { updateProgress() }

    private fun updateProgress() {
        val bytesAndStatus = getBytesAndStatus(downloadId)
        downLoadHandler!!.sendMessage(downLoadHandler!!.obtainMessage(HANDLE_DOWNLOAD, bytesAndStatus[0], bytesAndStatus[1], bytesAndStatus[2]))
    }

    /*
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     */
    private fun getBytesAndStatus(downloadId: Long): IntArray {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val bytesAndStatus = intArrayOf(-1, -1, 0)
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                //已经下载文件大小
                bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                //下载文件的总大小
                bytesAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                //下载状态
                bytesAndStatus[2] = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            }
        }
        return bytesAndStatus
    }

    @SuppressLint("HandlerLeak")
    var downLoadHandler: Handler? = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (onProgressListener != null && HANDLE_DOWNLOAD == msg.what) {
                //被除数可以为0，除数必须大于0
                if (msg.arg1 >= 0 && msg.arg2 > 0) {
                    onProgressListener!!.onProgress(msg.arg1 / msg.arg2.toFloat())
                }
            }
        }
    }

    interface OnProgressListener {
        fun onProgress(fraction: Float)
    }

    fun setOnProgressListener(onProgressListener: OnProgressListener) {
        this.onProgressListener = onProgressListener
    }
}