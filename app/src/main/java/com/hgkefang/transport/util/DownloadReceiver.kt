package com.hgkefang.transport.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.AppUtils
import java.io.File

/**
 * Create by admin on 2018/9/17
 * 下载广播接受
 */
class DownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            installApk(context, id)
        } else if (intent?.action.equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
            val viewDownloadIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            viewDownloadIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context?.startActivity(viewDownloadIntent)
        }
    }

    private fun installApk(context: Context?, id: Long?) {
        val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//        val downloadFileUri = downloadManager.getUriForDownloadedFile(id!!)
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id!!))
        if (cursor != null) {
            cursor.moveToFirst()
            val fileNameIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val fileName = cursor.getString(fileNameIdx)
            val file = File(fileName)
            cursor.close()
            AppUtils.installApp(file)
        }
    }
}