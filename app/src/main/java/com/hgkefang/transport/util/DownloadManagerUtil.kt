package com.hgkefang.transport.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

/**
 * Create by admin on 2018/9/17
 * 下载管理器
 */
class DownloadManagerUtil(private val context: Context) {

    fun download(url: String, title: String, desc: String): Long {
        val uri = Uri.parse(url)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(uri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setAllowedOverRoaming(false)
        request.setVisibleInDownloadsUi(true)
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, title)
        request.setTitle(title)
        request.setDescription(desc)
        request.setMimeType("application/vnd.android.package-archive")
        return downloadManager.enqueue(request)
    }

    /**
     * 下载前先移除前一个任务，防止重复下载
     */
    fun clearCurrentTask(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
    }

    /**
     * 获取下载进度
     */
    fun getDownloadPercent(downloadId: Long):Int{
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()){
            val downloadBytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalBytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val totalBytes = cursor.getLong(totalBytesIdx)
            val downloadBytes = cursor.getLong(downloadBytesIdx)
            return (downloadBytes * 100 / totalBytes).toInt()
        }
        return 0
    }
}