package com.hgkefang.transport.fragment

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.TimeUtils
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.gprinter.command.EscCommand
import com.hgkefang.transport.BluetoothActivity
import com.hgkefang.transport.R
import com.hgkefang.transport.adapter.HistoryOrderAdapter
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.net.API_LINEN_TYPE
import com.hgkefang.transport.net.API_ORDER
import com.hgkefang.transport.service.PrinterService
import com.hgkefang.transport.util.DeviceConnFactoryManager
import com.hgkefang.transport.util.TimeUtil
import kotlinx.android.synthetic.main.view_common.*
import org.jetbrains.anko.support.v4.toast
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Create by admin on 2018/9/4
 * 订单
 */
class HistoryOrderFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, HistoryOrderAdapter.OnSelectListener {

    private var pageValue = 0
    private var currentPage = 1
    private var hasMore = true
    private var isLoading = false
    private var results: ArrayList<RetData>? = null
    private lateinit var adapter: HistoryOrderAdapter
    private lateinit var myService: PrinterService

    companion object {
        fun getInstance(pageState: Int): HistoryOrderFragment {
            val historyOrderFragment = HistoryOrderFragment()
            val bundle = Bundle()
            bundle.putInt("pageState", pageState)
            historyOrderFragment.arguments = bundle
            return historyOrderFragment
        }

        const val BLUETOOTH_REQUEST_CODE = 0x001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageValue = arguments!!.getInt("pageState")
    }

    override fun getLayoutID(): Int {
        return R.layout.view_common
    }

    override fun initialize() {
        activity!!.bindService(Intent(activity, PrinterService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        initSwipeRefreshLayout(swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener(this)
        typeResult = ArrayList()
        refreshData()
        getLineTypeData()
        rvContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (isLoading) {
                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE && hasMore) {
                    currentPage++
                    refreshData()
                }
            }
        })
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            myService = (service as PrinterService.MyBinder).getService()
            myService.setNotifyPrinterListener(object : PrinterService.NotifyPrinterListener {
                override fun onNotifyPrinter(state: Int) {
                    if (state == 1) {
                        sendReceiptWithResponse()
                    }
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName) {
        }
    }

    private fun refreshData() {
        if (!NetworkUtils.isConnected()) {
            toast(R.string.toast_no_net)
            return
        }
        if (currentPage == 1) {
            if (!swipeRefreshLayout!!.isRefreshing) {
                swipeRefreshLayout.post { swipeRefreshLayout!!.isRefreshing = true }
            }
        }
        isLoading = true
        val params = linkedMapOf("page" to currentPage,
                "hotel_id" to MyApplication.retData?.id,
                "type" to pageValue,
                "token" to MyApplication.token)
        API_ORDER.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_order$pageValue", body)
            isLoading = false
            cancelRefreshAnimation(swipeRefreshLayout)
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
                if (it.retData.isEmpty()) {
                    toast("暂无数据")
                }
                hasMore = it.retData.size == 15
                if (currentPage == 1) {
                    results = it.retData
                    adapter = HistoryOrderAdapter(results!!, this)
                    rvContent.adapter = adapter
                } else {
                    results!!.addAll(it.retData)
                    adapter.setNewData(results!!)
                }
            }
        }
    }

    override fun onRefresh() {
        currentPage = 1
        refreshData()
    }

    private var typeList= ArrayList<String>()

    override fun onPrinter(retData: RetData, position: Int) {
        if (!myService.hasConnectPrinter()) {
            startActivityForResult(Intent(activity, BluetoothActivity::class.java), BLUETOOTH_REQUEST_CODE)
            return
        }
        myService.setRetData(retData)
        btnReceiptPrint()
    }

    private lateinit var typeResult: ArrayList<RetData>
    private fun getLineTypeData() {
        showLoadingDialog()
        val params = LinkedHashMap<String, Any?>()
        params["hotel_id"] = MyApplication.retData?.id
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
                typeResult.addAll(it.retData)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode != BLUETOOTH_REQUEST_CODE) {
            return
        }
        myService.onActivityResult(data!!.getStringExtra("address"))
    }

    private fun btnReceiptPrint() {
        myService.doPrinter()
    }

    // 打印
    private fun sendReceiptWithResponse() {
        val retData = myService.getRetData() ?: return
        if (retData.tradition_data.contains("|")) {
            typeList = retData.tradition_data.split("|").toList() as ArrayList<String>
        } else {
            typeList.add(retData.tradition_data)
        }
        val esc = EscCommand()
        esc.addInitializePrinter()
        esc.addPrintAndFeedLines(2.toByte())
        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT)
        esc.addText("下单时间：${TimeUtil.strTime2Date(retData!!.tradition_addtime, "yyyy-MM-dd HH:mm:ss")}\n")
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER)
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF)
        esc.addText("HG客房管家\n\n")

        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF)
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT)
        esc.addPrintAndLineFeed()
        esc.addText("酒店名称：${MyApplication.retData?.tradition_hotel_name}\n")
        esc.addText("订单号：${retData?.tradition_ordernumber}\n")
        esc.addText("经手人：${MyApplication.name}\n")
        if (!MyApplication.retData?.floor_name.isNullOrEmpty()) {
            esc.addText(String.format("%s%s\n", getString(R.string.category_name_), MyApplication.retData?.floor_name))
        }
        when (retData?.tradition_order_type) {
            "1" -> esc.addText("布草类型：${getString(R.string.send_linen)}\n")
            "2" -> esc.addText("布草类型：${getString(R.string.pick_linen)}\n")
            "3" -> esc.addText("布草类型：${getString(R.string.pollution)}\n")
            "4" -> esc.addText("布草类型：${getString(R.string.rewash_linen)}\n")
        }
        if (retData?.tradition_order_type == "1") {
            esc.addText(String.format("%s%s - %s\n", getString(R.string.linen_trend_),
                    retData?.tradition_hotel_name ?: "酒店", retData?.tradition_wash_name ?: "洗涤厂"))
        } else {
            esc.addText(String.format("%s%s - %s\n", getString(R.string.linen_trend_),
                    retData?.tradition_wash_name ?: "洗涤厂", retData?.tradition_hotel_name ?: "酒店"))
        }
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF)
        esc.addText("------------------------\n")
        esc.addText("商品信息：\n")
        esc.addPrintAndLineFeed()

        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF)
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT)
        esc.addText("\t规格\t\t\t\t数量\n")
        esc.addPrintAndLineFeed()
        var totalLinenCount = 0
        for (result in typeList) {
            val sb = StringBuilder()
            totalLinenCount += result.split("-")[1].toInt()
            for (retData in typeResult) {
                retData.son.forEach {
                    if (it.id == result.split("-")[0]) {
                        var s = "${it.tradition_name}-${it.tradition_spec}"
                        val stringBuilder = StringBuilder(s)
                        if (s.length < 30) {
                            for (i in 0 until (30 - s.length)) {
                                stringBuilder.append(" ")
                            }
                            s = stringBuilder.toString()
                        }
                        sb.append(String.format("%s\t x%s", s, result.split("-")[1]))
                    }
                }
            }
            esc.addText(sb.toString() + " \n")
        }

        esc.addPrintAndLineFeed()
        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT)
        esc.addText("总数量：$totalLinenCount      \n")
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF)
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT)
        esc.addText("------------------------\n")

        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF)
        esc.addText("操作时间：" + TimeUtils.millis2String(System.currentTimeMillis(), SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE) as DateFormat) + "\n")

        esc.addPrintAndFeedLines(4.toByte())
        esc.addQueryPrinterStatus()
        val data = esc.command
        DeviceConnFactoryManager.deviceConnFactoryManagers[PrinterService.id]!!.sendDataImmediately(data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity!!.unbindService(serviceConnection)
    }
}