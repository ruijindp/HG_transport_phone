package com.hgkefang.transport

import android.view.View
import com.hgkefang.transport.adapter.OrderPageAdapter
import kotlinx.android.synthetic.main.activity_history_order.*
import kotlinx.android.synthetic.main.view_title.*

/**
 * Create by admin on 2018/9/4
 * 历史订单
 */
class HistoryOrderActivity : BaseActivity(), View.OnClickListener {
    override fun getLayoutID(): Int {
        return R.layout.activity_history_order
    }

    override fun initialize() {
        tvPageTitle.text = getString(R.string.order)
        viewPager.adapter = OrderPageAdapter(supportFragmentManager)
        tabLayout.setupWithViewPager(viewPager)
        ivPageBack.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivPageBack -> finish()
        }
    }
}
