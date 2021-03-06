package com.hgkefang.transport

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.ObjectResult
import com.hgkefang.transport.net.API_ZXING
import kotlinx.android.synthetic.main.activity_scanning.*
import org.jetbrains.anko.toast

/**
 * Create by admin on 2018/9/14
 * 扫描代理
 */
class ProxyActivity : BaseActivity() {
    companion object {
        var isFinish = false
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_scanning
    }

    override fun initialize(savedInstanceState: Bundle?) {
        hasNeedFitWindow = true
        MyApplication.retData = null

        if (!hasFlash()) {
            ivFlash.visibility = View.GONE
        } else {
            ivFlash.visibility = View.VISIBLE
        }

        startScanning()
    }

    private fun hasFlash(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result.contents.isNullOrEmpty()) {
            toast("无法识别的内容")
            startScanning()
            return
        }
        commitData(result.contents)
    }

    private fun commitData(content: String) {
        showLoadingDialog()
        val params = LinkedHashMap<String, Any?>()
        params["code"] = content
        params["token"] = MyApplication.token
        Log.i("doScanning", params.toString())
        API_ZXING.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_scanning", body)
            dismissDialog()
            if (statusCode != 200) {
                startScanning()
                toast("网络错误：$statusCode")
                return@httpPost
            }
            if (isJsonArrayType(body)){
                startScanning()
                toast(getJsonMessage(body))
                return@httpPost
            }
            Gson().fromJson<ObjectResult>(body, ObjectResult::class.java).let {
                if (it.errMsg.code == 301) {
                    tokenInvalid()
                    return@httpPost
                }
                if (it.errMsg.code != 200) {
                    toast(it.message)
                    startScanning()
                    return@httpPost
                }
                MyApplication.retData = it.retData
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun startScanning() {
        IntentIntegrator(this)
                .setCaptureActivity(ScanningActivity::class.java)
                .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                .setPrompt("请对准需要识别酒店的二维码")
                .setCameraId(0)
                .setBeepEnabled(true)
                .initiateScan()
    }

    override fun onResume() {
        super.onResume()
        if (isFinish) {
            isFinish = false
            finish()
        }
    }
}