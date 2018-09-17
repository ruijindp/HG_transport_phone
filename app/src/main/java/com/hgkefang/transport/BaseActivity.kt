package com.hgkefang.transport

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.TimeUtils
import com.bronze.kutil.Param
import com.gyf.barlibrary.ImmersionBar
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.net.APP_PORT
import com.hgkefang.transport.util.AESUtil
import com.hgkefang.transport.util.SecretUtil
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Create by admin on 2018/9/4
 * 基类
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var loadingDialog: Dialog
    protected var hasNeedFitWindow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutID())

        initialize(savedInstanceState)
        if (!hasNeedFitWindow) {
            ImmersionBar.with(this).fitsSystemWindows(true).statusBarDarkFont(true).init()
        } else {
            ImmersionBar.with(this).init()
        }
    }

    abstract fun getLayoutID(): Int

    abstract fun initialize(savedInstanceState: Bundle?)

    protected fun initSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        swipeRefreshLayout.setProgressViewOffset(false, 1, 8)
    }

    protected fun cancelRefreshAnimation(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.postDelayed({
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }, 1000)
    }

    protected fun showLoadingDialog() {
        loadingDialog = Dialog(this@BaseActivity, R.style.AppTheme_Dialog)
        loadingDialog.setContentView(R.layout.dialog_loading)
        loadingDialog.setCancelable(false)
        loadingDialog.show()
    }

    protected fun dismissDialog() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getRequestParams(business: String): Param {
        val currentTime = TimeUtils.millis2String(System.currentTimeMillis(), SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE) as DateFormat)
        val nonceStr = SecretUtil.get32MD5Str("${SecretUtil.getNumLargeSmallLetter(10)}${System.currentTimeMillis()}")
        val iv = getIV(currentTime, nonceStr)

        val param = Param()
        param.put("business", AESUtil.encode(business, iv.substring(5, 21), iv))
        param.put("nonce_str", nonceStr)
        param.put("source_type", APP_PORT)
        param.put("spbill_create_ip", NetworkUtils.getIPAddress(true))
        param.put("time", currentTime)
        val sign = SecretUtil.get32MD5Str("${hash2String(param)}@$nonceStr").toUpperCase()
        param.put("sign", sign)
        return param
    }

    private fun getIV(time: String, nonce: String): String {
        val iv = APP_PORT + time + nonce + NetworkUtils.getIPAddress(true)
        return SecretUtil.get32MD5Str(SecretUtil.get32MD5Str(iv).toUpperCase()).toUpperCase()
    }

    private fun hash2String(signParam: Param): String {
        val param = signParam.toPostParam()
        return param.substring(1, param.length)
    }

    protected fun tokenInvalid() {
        toast(R.string.toast_token_invalid)
        MyApplication.token = null
        SPUtils.getInstance(Activity.MODE_PRIVATE).remove("token")
        val intent = Intent(this@BaseActivity, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        hasNeedFitWindow = false
        ImmersionBar.with(this).destroy()
    }

    //判断是数组还是对象
    protected fun isJsonArrayType(json: String?): Boolean {
        val jsonObject = JSONObject(json)
        val retData = JSONTokener(jsonObject.getString("retData")).nextValue()
        return retData is JSONArray
    }

    protected fun getJsonMessage(json: String?): String{
        val jsonObject = JSONObject(json)
        return jsonObject.getString("message")
    }
}