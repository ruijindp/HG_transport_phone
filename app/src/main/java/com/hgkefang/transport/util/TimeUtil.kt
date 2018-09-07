package com.hgkefang.transport.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Create by admin on 2018/9/5
 */
object TimeUtil {

    //时间转换为日期
    fun strTime2Date(time: String, format: String): String {
        @SuppressLint("SimpleDateFormat")
        val sdr = SimpleDateFormat(format)
        val i = Integer.parseInt(time)
        return sdr.format(Date(i * 1000L))
    }

    //获取时间差
    fun getTimeSpan(beginTime: String, endTime: String): Long {
        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val beginDate = sdf.parse(beginTime)
        val endDate = sdf.parse(endTime)
        val diff = beginDate.time - endDate.time
        return diff / (1000 * 60 * 60 * 24)
    }
}