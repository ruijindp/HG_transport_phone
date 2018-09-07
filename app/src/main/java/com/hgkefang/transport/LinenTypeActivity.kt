package com.hgkefang.transport

import android.content.Intent
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.View
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.hgkefang.transport.adapter.LinenAdapter
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.net.API_LINEN_TYPE
import kotlinx.android.synthetic.main.activity_linen_type.*
import kotlinx.android.synthetic.main.view_common.*
import kotlinx.android.synthetic.main.view_title.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.toast

/**
 * Create by admin on 2018/9/4
 * 布草
 */
class LinenTypeActivity : BaseActivity(), View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private lateinit var addResult: ArrayList<EvenBusEven>

    override fun getLayoutID(): Int {
        return R.layout.activity_linen_type
    }

    override fun initialize() {
        ivPageBack.setOnClickListener(this)
        tvCommitOrder.setOnClickListener(this)
        tvLinenCount.text = String.format(getString(R.string.dirty_count), 0)

        when (intent.getIntExtra("pageValue", -1)) {
            1 -> tvPageTitle.text = getString(R.string.send_linen)
            2 -> tvPageTitle.text = getString(R.string.pick_linen)
            3 -> tvPageTitle.text = getString(R.string.pollution)
            4 -> tvPageTitle.text = getString(R.string.rewash_linen)
        }

        addResult = ArrayList()
        initSwipeRefreshLayout(swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener(this)

        refreshData()
    }

    private fun refreshData() {
        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }
        }
        val params = LinkedHashMap<String, Any?>()
        params["hotel_id"] = MyApplication.hotel_id
        params["token"] = MyApplication.token
        API_LINEN_TYPE.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_linen", body)
            cancelRefreshAnimation(swipeRefreshLayout)
            if (statusCode != 200) {
                toast("网络错误：$statusCode")
                return@httpPost
            }
            Gson().fromJson<CommonResult>(body, CommonResult::class.java).let { it ->
                if (it.errMsg.code == 301) {
                    tokenInvalid()
                    return@httpPost
                }
                if (it.errMsg.code != 200) {
                    toast(it.message)
                    return@httpPost
                }
                var totalCount = 0
                it.retData.map {
                    totalCount += it.num
                }
                tvLinenCount.text = String.format(getString(R.string.dirty_count), totalCount)
                rvContent.adapter = LinenAdapter(it.retData)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivPageBack -> finish()
            R.id.tvCommitOrder -> {
                if (addResult.size == 0) {
                    toast(R.string.toast_no_linen)
                    return
                }
                Intent(this@LinenTypeActivity, ConfirmOrderActivity::class.java).let {
                    it.putExtra("linen", Gson().toJson(addResult))
                    it.putExtra("pageValue", intent.getIntExtra("pageValue", -1))
                    startActivity(it)
                }
            }
        }
    }

    override fun onRefresh() {
        refreshData()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EvenBusEven) {
        var totalLinen = 0
        if (addResult.size == 0) {
            addResult.add(event)
            tvLinenCount.text = String.format(getString(R.string.dirty_count), event.count)
            return
        }
        addResult.map {
            if (it.son.id == event.son.id) {
                it.count = event.count
                return
            }
        }
        addResult.add(event)
    }
}