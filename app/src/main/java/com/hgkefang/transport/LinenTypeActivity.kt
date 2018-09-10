package com.hgkefang.transport

import android.content.Intent
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.hgkefang.transport.adapter.LinenTypeAdapter
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.net.API_LINEN_TYPE
import kotlinx.android.synthetic.main.activity_linen_type.*
import kotlinx.android.synthetic.main.view_title.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.toast

/**
 * Create by admin on 2018/9/4
 * 布草
 */
class LinenTypeActivity : BaseActivity(), View.OnClickListener {

    private lateinit var addResult: ArrayList<EvenBusEven>

    override fun getLayoutID(): Int {
        return R.layout.activity_linen_type
    }

    override fun initialize() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        ivPageBack.setOnClickListener(this)
        tvCommitOrder.setOnClickListener(this)
        tvLinenCount.text = String.format(getString(R.string.total_linen), 0)

        when (intent.getIntExtra("pageValue", -1)) {
            1 -> tvPageTitle.text = getString(R.string.send_linen)
            2 -> tvPageTitle.text = getString(R.string.pick_linen)
            3 -> tvPageTitle.text = getString(R.string.pollution)
            4 -> tvPageTitle.text = getString(R.string.rewash_linen)
        }

        addResult = ArrayList()

        refreshData()
    }

    private fun refreshData() {
        showLoadingDialog()
        val params = LinkedHashMap<String, Any?>()
        params["hotel_id"] = MyApplication.hotel_id
        params["token"] = MyApplication.token
        API_LINEN_TYPE.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_linen", body)
            dismissDialog()
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
                rvContent.adapter = LinenTypeAdapter(it.retData)
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
        var hasExist = false
        var totalCount = 0
        addResult.map {
            if (it.son.id == event.son.id) {
                it.count = event.count
                hasExist = true
            }
        }
        if (!hasExist)
            addResult.add(event)
        addResult.map {
            totalCount += it.count
        }
        tvLinenCount.text = String.format(getString(R.string.total_linen), totalCount)
    }
}