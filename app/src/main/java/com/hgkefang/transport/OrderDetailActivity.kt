package com.hgkefang.transport

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.gprinter.command.EscCommand
import com.hgkefang.transport.adapter.LinenInfoAdapter
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.CommonResult
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.net.API_LINEN_TYPE
import com.hgkefang.transport.util.DeviceConnFactoryManager
import com.hgkefang.transport.util.PrinterCommand
import com.hgkefang.transport.util.ThreadPool
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

    private val BLUETOOTH_REQUEST_CODE = 0x001
    private val CONN_STATE_DISCONNECT = 0x007
    private val PRINTER_COMMAND_ERROR = 0x008
    private val ACTION_QUERY_PRINTER_STATE = "action_query_printer_state"
    private val CONN_STATE_FAILED = CONN_STATE_DISCONNECT shl 2
    private val CONN_PRINTER = 0x12
    private val MESSAGE_UPDATE_PARAMETER = 0x009
    private val MESSAGE_PUT = 0x002

    private val id = 1
    private var threadPool: ThreadPool? = null
    private var retData: RetData? = null
    private val spUtils = SPUtils.getInstance(Activity.MODE_PRIVATE)

    override fun getLayoutID(): Int {
        return R.layout.activity_order_detail
    }

    private val pageValue: Int
        get() {
            return intent.getIntExtra("pageValue", -1)
        }

    private lateinit var typeList: ArrayList<String>
    override fun initialize(savedInstanceState: Bundle?) {
        tvPageTitle.text = getString(R.string.order_detail)
        retData = intent.getSerializableExtra("retData") as RetData
        val pageValue = pageValue

        typeList = ArrayList()
        if (retData!!.tradition_data.contains("|")) {
            typeList = retData!!.tradition_data.split("|").toList() as ArrayList<String>
        } else {
            typeList.add(retData!!.tradition_data)
        }

        if(!MyApplication.retData?.floor_name.isNullOrEmpty()){
            tvCategory.visibility = View.VISIBLE
            tvCategory.text = String.format("%s%s", getString(R.string.category_name_), MyApplication.retData?.floor_name)
        }
        tvPrincipal.text = String.format("%s%s", getString(R.string.principal), MyApplication.name)
        if (pageValue == 1) {
            tvLinenTrend.text = String.format("%s%s - %s", getString(R.string.linen_trend_), retData!!.tradition_hotel_name, retData!!.tradition_wash_name)
        } else {
            tvLinenTrend.text = String.format("%s%s - %s", getString(R.string.linen_trend_), retData!!.tradition_wash_name, retData!!.tradition_hotel_name)
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

        if (!MyApplication.hasConnectPrinter)
            connectBle()
    }

    private fun connectBle() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            return
        }
        if (TextUtils.isEmpty(spUtils.getString("macAddress", ""))) {
            return
        }
        Thread(Runnable {
            Looper.prepare()
            Handler().post(runnable)
            Looper.loop()
        }).start()
    }

    private val runnable = Runnable {
        DeviceConnFactoryManager.Build()
                .setId(id)
                .setConnMethod(DeviceConnFactoryManager.ConnectType.BLUETOOTH)
                .setMacAddress(spUtils.getString("macAddress", ""))
                .build()
        DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.openPort()
        if (!DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.connState) {
            ToastUtils.showShort("未找到打印机")
            runOnUiThread { tvConnectPrinter.text = "连接打印机" }
        }
    }


    private lateinit var typeResult: ArrayList<RetData>
    private fun getLineTypeData() {
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
                typeResult = it.retData
                rvContent!!.setHasFixedSize(true)
                rvContent!!.isNestedScrollingEnabled = false
                rvContent!!.adapter = LinenInfoAdapter(retData!!.tradition_data, it.retData)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivPageBack -> finish()
            R.id.tvConnectPrinter -> {
                when (tvConnectPrinter.text.toString()) {
                    "连接打印机" -> bluetoothConnect()
                    "断开打印机" -> bluetoothDisconnect()
                }
            }
            R.id.tvPrinter -> btnReceiptPrint()
        }
    }

    override fun onStart() {
        super.onStart()
        if(!MyApplication.hasConnectPrinter){
            val filter = IntentFilter()
            filter.addAction(ACTION_QUERY_PRINTER_STATE)
            filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE)
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!MyApplication.hasConnectPrinter){
            unregisterReceiver(receiver)
//            DeviceConnFactoryManager.closeAllPort()
        }
        if (threadPool != null) {
            threadPool!!.stopThreadPool()
        }
    }

    // 打印机蓝牙连接
    private fun bluetoothConnect() {
        startActivityForResult(Intent(this@OrderDetailActivity, BluetoothActivity::class.java), BLUETOOTH_REQUEST_CODE)
    }

    // 打印机断开连接
    private fun bluetoothDisconnect() {
        mHandler.obtainMessage(CONN_STATE_DISCONNECT).sendToTarget()
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
                            tvConnectPrinter.text = "连接打印机"
                            MyApplication.hasConnectPrinter = false
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTING -> {
                            ToastUtils.showShort("打印机连接中...")
                            tvConnectPrinter.text = "连接打印机"
                            MyApplication.hasConnectPrinter = false
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTED -> {
                            ToastUtils.showShort("打印机已连接")
                            tvConnectPrinter.text = "断开打印机"
                            mHandler.obtainMessage(MESSAGE_PUT).sendToTarget()
                            MyApplication.hasConnectPrinter = true
                        }
                        CONN_STATE_FAILED -> {
                            ToastUtils.showShort("打印机连接失败")
                            tvConnectPrinter.text = "连接打印机"
                            MyApplication.hasConnectPrinter = false
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
                            .setConnMethod(DeviceConnFactoryManager.ConnectType.WIFI)
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
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode != BLUETOOTH_REQUEST_CODE) {
            return
        }
        macAddress = data!!.getStringExtra("address")
        DeviceConnFactoryManager.Build()
                .setId(id)
                .setConnMethod(DeviceConnFactoryManager.ConnectType.BLUETOOTH)
                .setMacAddress(macAddress!!)
                .build()
        DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.openPort()

        Handler().postDelayed({ btnReceiptPrint() }, 500)
    }

    private fun btnReceiptPrint() {
        if (DeviceConnFactoryManager.deviceConnFactoryManagers[id] == null || !DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.connState) {
            ToastUtils.showShort("请先连接打印机")
            bluetoothConnect()
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
        esc.addText("酒店名称：${MyApplication.retData?.tradition_hotel_name}\n")
        esc.addText("订单号：${retData!!.tradition_ordernumber}\n")
        esc.addText("负责人：${MyApplication.name}\n")
        if (pageValue == 1) {
            esc.addText(String.format("%s%s - %s\n", getString(R.string.linen_trend_), retData!!.tradition_hotel_name, retData!!.wash_name))
        } else {
            esc.addText(String.format("%s%s - %s\n", getString(R.string.linen_trend_), retData!!.wash_name, retData!!.tradition_hotel_name))
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
                    if (it.id == result.split("-")[0]) {
                        var linenType = "${it.tradition_name}${it.tradition_spec}"
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
        DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.sendDataImmediately(data)
    }
}