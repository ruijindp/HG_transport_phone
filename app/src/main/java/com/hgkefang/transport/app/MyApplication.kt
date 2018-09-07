package com.hgkefang.transport.app

import android.app.Activity
import android.app.Application
import android.content.Context
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.Utils
import kotlin.properties.Delegates

class MyApplication : Application() {

    companion object {
        var token: String? = null
        const val hotel_id = 7
        var name: String? = null
        var context: Context by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
        context = this
        token = SPUtils.getInstance(Activity.MODE_PRIVATE).getString("token", "")
        name = SPUtils.getInstance(Activity.MODE_PRIVATE).getString("name", "")
    }
}