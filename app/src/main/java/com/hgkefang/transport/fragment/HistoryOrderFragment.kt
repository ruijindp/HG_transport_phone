package com.hgkefang.transport.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.gprinter.command.EscCommand
import com.hgkefang.transport.BluetoothActivity
import com.hgkefang.transport.R
import com.hgkefang.transport.adapter.HistoryOrderAdapter
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.OrderEvent
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.net.API_LINEN_TYPE
import com.hgkefang.transport.net.API_ORDER
import com.hgkefang.transport.util.DeviceConnFactoryManager
import com.hgkefang.transport.util.PrinterCommand
import com.hgkefang.transport.util.ThreadPool
import com.hgkefang.transport.util.TimeUtil
import kotlinx.android.synthetic.main.view_common.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.support.v4.toast
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Create by admin on 2018/9/4
 * 订单
 */
class HistoryOrderFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private val BLUETOOTH_REQUEST_CODE = 0x001
    private val CONN_STATE_DISCONNECT = 0x007
    private val PRINTER_COMMAND_ERROR = 0x008
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private val ACTION_QUERY_PRINTER_STATE = "action_query_printer_state"
    private val CONN_STATE_FAILED = CONN_STATE_DISCONNECT shl 2
    private val CONN_PRINTER = 0x12
    val MESSAGE_UPDATE_PARAMETER = 0x009
    private val MESSAGE_PUT = 0x002

    internal val id = 1
    private var threadPool: ThreadPool? = null
    private var retData: RetData? = null
    private val spUtils = SPUtils.getInstance(Activity.MODE_PRIVATE)

    private var pageValue = 0
    private var currentPage = 1
    private var hasMore = true
    private var isLoading = false
    private var results: ArrayList<RetData>? = null
    private lateinit var adapter: HistoryOrderAdapter

    companion object {
        fun getInstance(pageState: Int): HistoryOrderFragment {
            val historyOrderFragment = HistoryOrderFragment()
            val bundle = Bundle()
            bundle.putInt("pageState", pageState)
            historyOrderFragment.arguments = bundle
            return historyOrderFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageValue = arguments!!.getInt("pageState")
    }

    override fun getLayoutID(): Int {
        return R.layout.view_common
    }

    override fun initialize() {
        initSwipeRefreshLayout(swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener(this)
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

    private fun refreshData() {
        if (currentPage == 1) {
            if (!swipeRefreshLayout!!.isRefreshing) {
                swipeRefreshLayout.post { swipeRefreshLayout!!.isRefreshing = true }
            }
        }
        isLoading = true
        val params = LinkedHashMap<String, Any>()
        params["page"] = currentPage
        if (pageValue != 0)
            params["type"] = pageValue
        params["token"] = MyApplication.token!!
        Log.i("requestParam", params.toString())
        API_ORDER.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_order$pageValue", body)
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
                    adapter = HistoryOrderAdapter(pageValue, results!!)
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

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(ACTION_QUERY_PRINTER_STATE)
        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE)
        requireActivity().registerReceiver(receiver, filter)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private lateinit var typeList : ArrayList<String>
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OrderEvent) {
        retData = event.retData
        typeList = ArrayList()
        if (event.retData.tradition_data.contains("|")) {
            typeList = event.retData.tradition_data.split("|").toList() as ArrayList<String>
        } else {
            typeList.add(event.retData.tradition_data)
        }
        btnReceiptPrint()
    }

    private lateinit var typeResult : ArrayList<RetData>
    private fun getLineTypeData() {
        showLoadingDialog()
        val params = java.util.LinkedHashMap<String, Any?>()
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
                typeResult = it.retData
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> mHandler.obtainMessage(CONN_STATE_DISCONNECT).sendToTarget()
                DeviceConnFactoryManager.ACTION_CONN_STATE -> {
                    val state = intent.getIntExtra(DeviceConnFactoryManager.STATE, -1)
                    val deviceId = intent.getIntExtra(DeviceConnFactoryManager.DEVICE_ID, -1)
                    when (state) {
                        DeviceConnFactoryManager.CONN_STATE_DISCONNECT -> if (id == deviceId) {
                            ToastUtils.showShort("打印机断开")
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTING -> {
                            ToastUtils.showShort("打印机连接中...")
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTED -> {
                            ToastUtils.showShort("打印机已连接")
                            mHandler.obtainMessage(MESSAGE_PUT).sendToTarget()
                        }
                        CONN_STATE_FAILED -> {
                            ToastUtils.showShort("打印机连接失败")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    internal var mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: android.os.Message) {
            when (msg.what) {
                CONN_STATE_DISCONNECT -> if (DeviceConnFactoryManager.deviceConnFactoryManagers[id] != null) {
                    DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.closePort(id)
                }
                PRINTER_COMMAND_ERROR -> ToastUtils.showShort("请选择正确的打印机指令")
                CONN_PRINTER -> ToastUtils.showShort("请先连接打印机")
                MESSAGE_UPDATE_PARAMETER -> {
                    val strIp = msg.data.getString("Ip")
                    val strPort = msg.data.getString("Port")
                    DeviceConnFactoryManager.Build()
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.WIFI)
                            .setIp(strIp!!)
                            .setId(id)
                            .setPort(Integer.parseInt(strPort!!))
                            .build()
                    threadPool = ThreadPool.instantiation
                    threadPool!!.addTask(Runnable { DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.openPort() })
                }
                MESSAGE_PUT -> if (TextUtils.isEmpty(spUtils.getString("macAddress", ""))) {
                    spUtils.put("macAddress", macAddress)
                }
            }
        }
    }

    private var macAddress: String? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK){
            return
        }
        if (requestCode != BLUETOOTH_REQUEST_CODE){
            return
        }
        macAddress = data!!.getStringExtra("address")
        DeviceConnFactoryManager.Build()
                .setId(id)
                .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH)
                .setMacAddress(macAddress!!)
                .build()
        DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.openPort()
    }

    private fun btnReceiptPrint() {
        if (DeviceConnFactoryManager.deviceConnFactoryManagers[id] == null ) {
            ToastUtils.showShort("请先连接打印机")
            startActivityForResult(Intent(requireActivity(), BluetoothActivity::class.java), BLUETOOTH_REQUEST_CODE)
            return
        }
        threadPool = ThreadPool.instantiation
        threadPool!!.addTask(Runnable {
            if (DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.getCurrentPrinterCommand() === PrinterCommand.ESC) {
                sendReceiptWithResponse()
            } else {
                mHandler.obtainMessage(PRINTER_COMMAND_ERROR).sendToTarget()
            }
        })
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
        esc.addText("订单号：${retData!!.tradition_ordernumber}\n")
        esc.addText("负责人：${MyApplication.name}\n")
        if (pageValue == 1) {
            esc.addText(String.format("%s%s - %s\n", getString(R.string.linen_trend_), retData!!.tradition_hotel_name, retData!!.tradition_wash_name))
        } else {
            esc.addText(String.format("%s%s - %s\n", getString(R.string.linen_trend_), retData!!.tradition_wash_name, retData!!.tradition_hotel_name))
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
                retData.son.map {
                    if (it.id == result.split("-")[0]){
                        sb.append(String.format("%s%s     x%s", it.tradition_name, it.tradition_spec, result.split("-")[1]))
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
        DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.sendDataImmediately(data)
    }
}