package com.hgkefang.transport

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.hgkefang.transport.adapter.LinenTypeAdapter
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.entity.ObjectResult
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.net.API_LINEN_TYPE
import com.hgkefang.transport.net.API_SHOE_TYPE
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
    private var currentTab = 1
    private var shoeId = -1

    override fun getLayoutID(): Int {
        return R.layout.activity_linen_type
    }

    override fun initialize(savedInstanceState: Bundle?) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        ivPageBack.setOnClickListener(this)
        tvCommitOrder.setOnClickListener(this)
        flLinen.setOnClickListener(this)
        flShoe.setOnClickListener(this)
        tvLinenCount.text = String.format(getString(R.string.total_linen), 0)

        when (intent.getIntExtra("pageValue", -1)) {
            1 -> tvPageTitle.text = getString(R.string.send_linen)
            2 -> tvPageTitle.text = getString(R.string.pick_linen)
            3 -> tvPageTitle.text = getString(R.string.pollution)
            4 -> tvPageTitle.text = getString(R.string.rewash_linen)
        }

        addResult = ArrayList()

//        refreshData()
        getShoeIDData()
    }

    private fun getShoeIDData() {
        showLoadingDialog()
        val params = LinkedHashMap<String, Any?>()
        params["token"] = MyApplication.token
        API_SHOE_TYPE.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_linen", body)
            if (statusCode != 200) {
                dismissDialog()
                toast("网络错误：$statusCode")
                return@httpPost
            }
            Gson().fromJson<ObjectResult>(body, ObjectResult::class.java).let { it ->
                if (it.errMsg.code == 301) {
                    tokenInvalid()
                    dismissDialog()
                    return@httpPost
                }
                if (it.errMsg.code != 200) {
                    toast(it.message)
                    dismissDialog()
                    return@httpPost
                }
                shoeId = it.retData.slipper
                refreshData()
            }
        }
    }

    private fun refreshData() {
//        showLoadingDialog()
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
                val linenResults = ArrayList<RetData>()
                val shoeResults = ArrayList<RetData>()
                it.retData.map {
                    if (it.id == shoeId.toString()){
                        shoeResults.add(it)
                    } else{
                        linenResults.add(it)
                    }
                }
                rvContent.adapter = LinenTypeAdapter(if (currentTab == 1) linenResults else shoeResults)
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
            R.id.flLinen -> {
                currentTab = 1
                view1.visibility = View.VISIBLE
                view2.visibility = View.INVISIBLE
                tvLinen.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                tvShoe.setTextColor(ContextCompat.getColor(this, R.color.black_2d))
                refreshData()
            }
            R.id.flShoe -> {
                currentTab = 2
                view1.visibility = View.INVISIBLE
                view2.visibility = View.VISIBLE
                tvShoe.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                tvLinen.setTextColor(ContextCompat.getColor(this, R.color.black_2d))
                refreshData()
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