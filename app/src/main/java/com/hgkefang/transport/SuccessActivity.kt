package com.hgkefang.transport

import android.content.Intent
import kotlinx.android.synthetic.main.activity_success.*

/**
 * Create by admin on 2018/9/5
 * 提交成功
 */
class SuccessActivity : BaseActivity() {

    override fun getLayoutID(): Int {
        return R.layout.activity_success
    }

    override fun initialize() {
        tvLinenCount.text = String.format(getString(R.string.commit_count), intent.getIntExtra("totalLinen", -1))
        tvBackMain.setOnClickListener {
            val intent = Intent(this@SuccessActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        tvSeeOrder.setOnClickListener {
            startActivity(Intent(this@SuccessActivity, HistoryOrderActivity::class.java))
            finish()
        }
    }
}