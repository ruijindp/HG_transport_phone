package com.hgkefang.transport

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.net.API_HOTEL_INFO
import com.hgkefang.transport.view.NamedEntityPopup
import kotlinx.android.synthetic.main.activity_hotel.*
import org.jetbrains.anko.toast

/**
 * Create by admin on 2018/9/5
 * 选择酒店
 */
class HotelActivity : BaseActivity(), View.OnClickListener, NamedEntityPopup.EntityPopupSelectListener {


    private lateinit var results: ArrayList<RetData>
    private lateinit var popup: NamedEntityPopup

    override fun getLayoutID(): Int {
        return R.layout.activity_hotel
    }

    override fun initialize(savedInstanceState: Bundle?) {
        MyApplication.retData = null
        refreshData()
        lnHotel.setOnClickListener(this)
        tvConfirm.setOnClickListener {
            doLogin()
        }
        tvSearch.setOnClickListener {
            refreshData()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.lnHotel -> {
                popup = NamedEntityPopup.create(lnHotel)
                popup.setEntityPopupSelectListener(this)
                popup.setEntities(results)
                popup.showAsDropDown(v)
            }
        }
    }

    override fun selectStringAt(popup: NamedEntityPopup, index: Int) {
        when (popup.lastAnchor!!.id) {
            R.id.lnHotel -> {
                tvHotel.text = results[index].tradition_hotel_name
                MyApplication.retData = results[index]
            }
        }
    }

    private fun refreshData() {
        showLoadingDialog()
        val params = LinkedHashMap<String, Any?>()
        params["hotel_name"] = etHotel.text.toString()
        params["token"] = MyApplication.token
        API_HOTEL_INFO.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            if(body.isNullOrEmpty()){
                toast("网络错误：$statusCode")
                return@httpPost
            }
            Log.i("response_hotel", body)
            dismissDialog()
            if (statusCode != 200) {
                toast("网络错误：$statusCode")
                return@httpPost
            }
            Gson().fromJson<CommonResult>(body, CommonResult::class.java).let {
                if (it.errMsg.code == 301) {
                    tokenInvalid()
                    return@httpPost
                }
                if (it.errMsg.code != 200) {
                    toast(it.message)
                    return@httpPost
                }
                results = it.retData
            }
        }
    }

    private fun doLogin() {
        if (MyApplication.retData == null) {
            toast("还没有选择酒店")
            return
        }
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//        ProxyActivity.isFinish = true
//        finish()
//    }
}