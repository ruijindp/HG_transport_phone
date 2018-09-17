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
        return downloadManager.enqueue(DownloadManager.Request(uri).let {
            it.setAllowedNetworkTypes(DownloadManager.PAUSED_QUEUED_FOR_WIFI)
            it.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            it.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, title)
            it.setTitle(title)
            it.setDescription(desc)
            it.setMimeType("application/vnd.android.package-archive")
        })
    }

    /**
     * 下载前先移除前一个任务，防止重复下载
     */
    fun clearCurrentTask(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
    }
}