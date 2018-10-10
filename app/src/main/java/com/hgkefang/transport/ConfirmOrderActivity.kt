package com.hgkefang.transport

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hgkefang.transport.adapter.ConfirmOrderAdapter
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.net.API_COMMIT_ORDER
import kotlinx.android.synthetic.main.activity_confirm_order.*
import kotlinx.android.synthetic.main.view_title.*
import org.jetbrains.anko.toast
import java.util.*
import kotlin.collections.ArrayList

/**
 * Create by admin on 2018/9/5
 * 确认订单
 */
class ConfirmOrderActivity : BaseActivity(), View.OnClickListener {

    private var pageValue = 0
    private var totalLinen = 0
    private var linen: String? = null
    private var linenType: String? = null

    override fun getLayoutID(): Int {
        return R.layout.activity_confirm_order
    }

    private val arrayList: ArrayList<EvenBusEven>
        get() {
            return Gson().fromJson<ArrayList<EvenBusEven>>(intent.getStringExtra("linen"), object : TypeToken<List<EvenBusEven>>() {}.type)
        }

    override fun initialize(savedInstanceState: Bundle?) {
        tvPageTitle.text = getString(R.string.confirm_order)
        pageValue = intent.getIntExtra("pageValue", -1)
        linenType = intent.getStringExtra("linenBody")
        if (!MyApplication.retData?.floor_name.isNullOrEmpty()) {
            tvCategory.visibility = View.VISIBLE
            tvCategory.text = String.format("%s%s", getString(R.string.category_name_), MyApplication.retData?.floor_name)
        }
//        val linenResult: ArrayList<EvenBusEven> = arrayList
        linen = intent.getStringExtra("linen")

        ivPageBack.setOnClickListener(this)
        tvConfirmOrder.setOnClickListener(this)

        rvContent.adapter = ConfirmOrderAdapter(sortResults(linenType!!))

        arrayList.forEach {
            totalLinen += it.count
        }
        tvLinenCount.text = String.format(getString(R.string.dirty_count), totalLinen)

        tvPrincipal.text = String.format("%s%s", getString(R.string.principal), MyApplication.name)
        if (pageValue == 1) {
            tvLinenTrend.text = String.format("%s%s - %s", getString(R.string.linen_trend_),
                    MyApplication.retData?.tradition_hotel_name
                            ?: "酒店", MyApplication.retData?.wash_name ?: "洗涤厂")
        } else {
            tvLinenTrend.text = String.format("%s%s - %s", getString(R.string.linen_trend_),
                    MyApplication.retData?.wash_name
                            ?: "洗涤厂", MyApplication.retData?.tradition_hotel_name ?: "酒店")
        }
    }

    private fun sortResults(linenType: String): ArrayList<EvenBusEven> {
        val commonResult = Gson().fromJson<CommonResult>(linenType, CommonResult::class.java)
        val result = ArrayList<EvenBusEven>()
        for (i in commonResult.retData.indices) {
            for (j in commonResult.retData[i].son.indices) {
                arrayList.forEach {
                    if (it.son.id == commonResult.retData[i].son[j].id) {
                        result.add(EvenBusEven(it.son, it.count))
                    }
                }
            }
        }
        return result
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivPageBack -> finish()
            R.id.tvConfirmOrder -> commitOrder()
        }
    }

    private fun commitOrder() {
        showLoadingDialog()
        val params = LinkedHashMap<String, Any?>()
        params["doaction"] = if (pageValue == 1) 1 else 2
        val sb = StringBuilder()
        sortResults(linenType!!).forEach {
            sb.append("${it.son.id}-${it.count}|")
        }
        params["data"] = sb.toString().substring(0, sb.length - 1)
        if (MyApplication.retData?.floor_name != null) {
            params["floor"] = MyApplication.retData?.floor_name
        }
        params["hotel_id"] = MyApplication.retData!!.id
        params["token"] = MyApplication.token
        params["tradition_state"] = 11
        params["type"] = pageValue
        API_COMMIT_ORDER.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_commit", body)
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
                toast(it.message)
                val intent = Intent(this@ConfirmOrderActivity, SuccessActivity::class.java)
                intent.putExtra("totalLinen", totalLinen)
                intent.putExtra("linen", linen)
                intent.putExtra("pageValue", pageValue)
                startActivity(intent)
                finish()
            }
        }
    }
}
