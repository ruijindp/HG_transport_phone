package com.hgkefang.transport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import com.blankj.utilcode.util.SPUtils
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.util.TimeUtil
import kotlinx.android.synthetic.main.activity_maturity.*
import org.jetbrains.anko.toast

/**
 * Create by admin on 2018/9/5
 * 已到期
 */
class MaturityActivity : BaseActivity() {

    override fun getLayoutID(): Int {
        return R.layout.activity_maturity
    }

    override fun initialize(savedInstanceState: Bundle?) {
        val maturityTime = intent.getStringExtra("time")
        tvDate.text = String.format(getString(R.string.maturity), TimeUtil.strTime2Date(maturityTime, "yyyy-MM-dd"))
        countDownTimer.start()
    }

    private val countDownTimer = object : CountDownTimer(5000, 1000) {
        override fun onTick(l: Long) {
            tvCountDown.text = (l / 1000).toString()
        }

        override fun onFinish() {
            MyApplication.token = null
            SPUtils.getInstance(Activity.MODE_PRIVATE).remove("token")
            val intent = Intent(this@MaturityActivity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
    }

    override fun onBackPressed() {
        toast("请稍等，将自动为你跳转！")
    }
}