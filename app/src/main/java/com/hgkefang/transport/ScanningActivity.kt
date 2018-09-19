package com.hgkefang.transport

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.ObjectResult
import com.hgkefang.transport.net.API_ZXING
import com.journeyapps.barcodescanner.CaptureManager
import kotlinx.android.synthetic.main.activity_scanning.*
import org.jetbrains.anko.toast

/**
 * Create by admin on 2018/9/14
 * 扫描
 */
class ScanningActivity : BaseActivity(), View.OnClickListener {
    private lateinit var captureManager: CaptureManager

    override fun getLayoutID(): Int {
        return R.layout.activity_scanning
    }

    override fun initialize(savedInstanceState: Bundle?) {
        hasNeedFitWindow = true
        MyApplication.retData = null
        ivFlash.setOnClickListener(this)
        if (!hasFlash()) {
            ivFlash.visibility = View.GONE
        } else {
            ivFlash.visibility = View.VISIBLE
        }
        lnSelectHotel.setOnClickListener {
            val intent = Intent(this, HotelActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        captureManager = CaptureManager(this, barcodeView)
        captureManager.setCallbackListener {
            if (it.text.isNullOrEmpty()){
                toast("二维码无法识别")
                return@setCallbackListener
            }
            commitData(it.text)
        }
        captureManager.initializeFromIntent(intent, savedInstanceState)
        captureManager.decode()
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
                toast("网络错误：$statusCode")
                return@httpPost
            }
            if (isJsonArrayType(body)) {
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
                    return@httpPost
                }
                MyApplication.retData = it.retData
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private var hasOpenFlash = false
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivFlash -> {
                if (hasOpenFlash) {
                    barcodeView.setTorchOff()
                } else {
                    barcodeView.setTorchOn()
                }
                hasOpenFlash = !hasOpenFlash
            }
        }
    }

    private fun hasFlash(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    override fun onResume() {
        super.onResume()
        captureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        captureManager.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        captureManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//        ProxyActivity.isFinish = true
//        finish()
//    }
}