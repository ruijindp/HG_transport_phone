package com.hgkefang.transport.util

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.ToastUtils
import com.hgkefang.transport.MainActivity


/**
 * Create by admin on 2018/9/17
 * 下载广播接受
 */
class DownloadReceiver(private val activity: Activity) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            installApk(context, id)
        } else if (intent?.action.equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
//            val viewDownloadIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
//            viewDownloadIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            context?.startActivity(viewDownloadIntent)
        }
    }

    private fun installApk(context: Context?, id: Long?) {
        val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadFileUri = downloadManager.getUriForDownloadedFile(id!!)
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
        if (activity is MainActivity){
            activity.closeContentScheduled()
        }
        if (cursor.moveToFirst()) {
            val fileNameIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val filePath = cursor.getString(fileNameIdx)
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(columnIndex)
            cursor.close()
            when(status){
                DownloadManager.STATUS_FAILED->{
                    ToastUtils.showLong("下载失败")
                    if (activity is MainActivity) {
                        activity.dismissDownloadDialog()
                    }
                }
                DownloadManager.STATUS_PAUSED->{}
                DownloadManager.STATUS_PENDING->{}
                DownloadManager.STATUS_RUNNING->{}
                DownloadManager.STATUS_SUCCESSFUL->{
                    if (activity is MainActivity) {
                        activity.dismissDownloadDialog()
                        activity.installApk(filePath)
                    }
                }
            }
        }
    }

}