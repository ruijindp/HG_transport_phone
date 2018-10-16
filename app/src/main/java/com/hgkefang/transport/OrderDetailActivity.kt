package com.hgkefang.transport

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import com.blankj.utilcode.util.TimeUtils
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.gprinter.command.EscCommand
import com.hgkefang.transport.adapter.LinenInfoAdapter
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.fragment.HistoryOrderFragment
import com.hgkefang.transport.net.API_LINEN_TYPE
import com.hgkefang.transport.service.PrinterService
import com.hgkefang.transport.util.DeviceConnFactoryManager
import com.hgkefang.transport.util.TimeUtil
import kotlinx.android.synthetic.main.activity_order_detail.*
import kotlinx.android.synthetic.main.view_title.*
import org.jetbrains.anko.toast
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Create by admin on 2018/9/5
 * 订单详情
 */
class OrderDetailActivity : BaseActivity(), View.OnClickListener {

    private var retData: RetData? = null
    private lateinit var myService: PrinterService

    override fun getLayoutID(): Int {
        return R.layout.activity_order_detail
    }

    private lateinit var typeList: ArrayList<String>
    override fun initialize(savedInstanceState: Bundle?) {
        bindService(Intent(this, PrinterService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        tvPageTitle.text = getString(R.string.order_detail)
        retData = intent.getSerializableExtra("retData") as RetData

        typeList = ArrayList()
        if (retData!!.tradition_data.contains("|")) {
            typeList = retData!!.tradition_data.split("|").toList() as ArrayList<String>
        } else {
            typeList.add(retData!!.tradition_data)
        }
        when (retData?.tradition_order_type) {
            "1" -> tvOrderType.text = String.format("%s%s", getString(R.string.order_type), getString(R.string.send_linen))
            "2" -> tvOrderType.text = String.format("%s%s", getString(R.string.order_type), getString(R.string.pick_linen))
            "3" -> tvOrderType.text = String.format("%s%s", getString(R.string.order_type), getString(R.string.pollution))
            "4" -> tvOrderType.text = String.format("%s%s", getString(R.string.order_type), getString(R.string.rewash_linen))
        }
        if (!retData?.tradition_floor_name.isNullOrEmpty()) {
            tvCategory.visibility = View.VISIBLE
            tvCategory.text = String.format("%s%s", getString(R.string.category_name_), retData?.tradition_floor_name)
        }
        tvPrincipal.text = String.format("%s%s", getString(R.string.principal), MyApplication.name)
        if (retData?.tradition_order_type == "1") {
            tvLinenTrend.text = String.format("%s%s - %s", getString(R.string.linen_trend_), retData?.tradition_hotel_name
                    ?: "酒店", retData?.tradition_wash_name ?: "洗涤厂")
        } else {
            tvLinenTrend.text = String.format("%s%s - %s", getString(R.string.linen_trend_), retData?.tradition_wash_name
                    ?: "洗涤厂", retData?.tradition_hotel_name ?: "酒店")
        }
        tvOrderNum.text = String.format("%s%s", getString(R.string.order_num), retData!!.tradition_ordernumber)
        tvOrderTime.text = String.format("%s%s", getString(R.string.order_time), TimeUtil.strTime2Date(retData!!.tradition_addtime, "yyyy-MM-dd HH:mm:ss"))
        tvTotalMoney.text = retData!!.tradition_price
        var totalCount = 0
        if (retData!!.tradition_data.contains("|")) {
            retData!!.tradition_data.split("|").map {
                totalCount += it.split("-")[1].toInt()
            }
        } else {
            totalCount = retData!!.tradition_data.split("-")[1].toInt()
        }
        tvTotalLinen.text = String.format(getString(R.string.total_linen), totalCount)
        ivPageBack.setOnClickListener(this)
        tvConnectPrinter.setOnClickListener(this)
        tvPrinter.setOnClickListener(this)

        getLineTypeData()
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

    private lateinit var typeResult: ArrayList<RetData>
    private fun getLineTypeData() {
        showLoadingDialog()
        val params = linkedMapOf("hotel_id" to MyApplication.retData?.id,
                "token" to MyApplication.token)
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
                typeResult = it.retData
                rvContent.setHasFixedSize(true)
                rvContent.isNestedScrollingEnabled = false
                rvContent.adapter = LinenInfoAdapter(retData!!.tradition_data, it.retData)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivPageBack -> finish()
            R.id.tvPrinter -> btnReceiptPrint()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode != HistoryOrderFragment.BLUETOOTH_REQUEST_CODE) {
            return
        }
        myService.onActivityResult(data!!.getStringExtra("address"))
    }

    private fun btnReceiptPrint() {
        if (!myService.hasConnectPrinter()) {
            startActivityForResult(Intent(this, BluetoothActivity::class.java), HistoryOrderFragment.BLUETOOTH_REQUEST_CODE)
            return
        }
        myService.doPrinter()
    }

    // 打印
    private fun sendReceiptWithResponse() {
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
                        var linenType = "${it.tradition_name}-${it.tradition_spec}"
                        val stringBuilder = StringBuilder(linenType)
                        if (linenType.length < 30) {
                            for (i in 0 until (30 - linenType.length)) {
                                stringBuilder.append(" ")
                            }
                            linenType = stringBuilder.toString()
                        }
                        sb.append(String.format("%s\t x%s", linenType, result.split("-")[1]))
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
}