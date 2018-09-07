package com.hgkefang.transport

import android.content.Intent
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

    override fun initialize() {
        refreshData()



        lnHotel.setOnClickListener(this)
        tvConfirm.setOnClickListener {
            doLogin()
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

    var retData : RetData ?= null

    override fun selectStringAt(popup: NamedEntityPopup, index: Int) {
        when(popup.lastAnchor!!.id){
            R.id.lnHotel -> {
                retData = results[index]
                tvHotel.text = results[index].tradition_hotel_name
            }
        }

    }

    private fun refreshData() {
        showLoadingDialog()
        val params = LinkedHashMap<String, Any>()
        params["hotel_name"] = etHotel.text.toString()
        params["token"] = MyApplication.token!!
        API_HOTEL_INFO.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_hotel", body)
            dismissDialog()
            if (statusCode != 200) {
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
        if (retData == null){
            toast("还没有选择酒店")
            return
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("retData", retData)
        startActivity(intent)
        finish()
    }
}