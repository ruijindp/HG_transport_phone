package com.hgkefang.transport

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import com.hgkefang.transport.app.MyApplication
import com.journeyapps.barcodescanner.CaptureManager
import kotlinx.android.synthetic.main.activity_scanning.*

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
            ProxyActivity.isFinish = true
            finish()
        }
        captureManager = CaptureManager(this, barcodeView)
        captureManager.initializeFromIntent(intent, savedInstanceState)
        captureManager.decode()
    }

    private var hasOpenFlash = false
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivFlash -> {
                if (hasOpenFlash) {
                    barcodeView.setTorchOn()
                } else {
                    barcodeView.setTorchOff()
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

    override fun onBackPressed() {
        super.onBackPressed()
        ProxyActivity.isFinish = true
        finish()
    }
}