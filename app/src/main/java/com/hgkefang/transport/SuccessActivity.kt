package com.hgkefang.transport

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gprinter.command.EscCommand
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.util.DeviceConnFactoryManager
import com.hgkefang.transport.util.PrinterCommand
import com.hgkefang.transport.util.ThreadPool
import kotlinx.android.synthetic.main.activity_order_detail.*
import kotlinx.android.synthetic.main.activity_success.*
import org.jetbrains.anko.toast
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Create by admin on 2018/9/5
 * 提交成功
 */
class SuccessActivity : BaseActivity() {

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
    private val spUtils = SPUtils.getInstance(Activity.MODE_PRIVATE)
    private var linenResult: ArrayList<EvenBusEven>? = null

    override fun getLayoutID(): Int {
        return R.layout.activity_success
    }

    override fun initialize(savedInstanceState: Bundle?) {
        linenResult = Gson().fromJson<EvenBusEven>(intent.getStringExtra("linen"), object : TypeToken<List<EvenBusEven>>(){}.type) as ArrayList<EvenBusEven>
        tvLinenCount.text = String.format(getString(R.string.commit_count), intent.getIntExtra("totalLinen", -1))
        tvBackMain.setOnClickListener {
            val intent = Intent(this@SuccessActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        tvSeeOrder.setOnClickListener {
            startActivity(Intent(this@SuccessActivity, HistoryOrderActivity::class.java))
            finish()
        }
        tvPrinterOrder.setOnClickListener {
            btnReceiptPrint()
        }
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

    private fun bluetoothConnect() {
        startActivityForResult(Intent(this@SuccessActivity, BluetoothActivity::class.java), BLUETOOTH_REQUEST_CODE)
    }

    override fun onStart() {
        super.onStart()
        if (!MyApplication.hasConnectPrinter) {
            val filter = IntentFilter()
            filter.addAction(ACTION_QUERY_PRINTER_STATE)
            filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE)
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!MyApplication.hasConnectPrinter) {
            unregisterReceiver(receiver)
        }
        if (threadPool != null) {
            threadPool!!.stopThreadPool()
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
                            MyApplication.hasConnectPrinter = false
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTING -> {
                            ToastUtils.showShort("打印机连接中...")
                            MyApplication.hasConnectPrinter = false
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTED -> {
                            ToastUtils.showShort("打印机已连接")
                            mHandler.obtainMessage(MESSAGE_PUT).sendToTarget()
                            MyApplication.hasConnectPrinter = true
                        }
                        CONN_STATE_FAILED -> {
                            ToastUtils.showShort("打印机连接失败")
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

        esc.addText("下单时间：${TimeUtils.millis2String(System.currentTimeMillis(), SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE) as DateFormat)}\n")

        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER)
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF)
        esc.addText("HG客房管家\n\n")

        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF)
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT)
        esc.addPrintAndLineFeed()
        esc.addText("酒店名称：${MyApplication.retData?.tradition_hotel_name}\n")
        esc.addText("负责人：${MyApplication.name}\n")
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF)
        esc.addText("------------------------\n")
        esc.addText("商品信息：\n")
        esc.addPrintAndLineFeed()

        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF)
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT)
        esc.addText("\t规格\t\t\t\t数量\n")
        esc.addPrintAndLineFeed()
        var totalLinenCount = 0
        for (result in linenResult!!) {
            val sb = StringBuilder()
            totalLinenCount += result.count
            var linenType = "${result.son.tradition_name}${result.son.tradition_spec}"
            val stringBuilder = StringBuilder(linenType)
            if (linenType.length < 30) {
                for (i in 0 until (30 - linenType.length)) {
                    stringBuilder.append(" ")
                }
                linenType = stringBuilder.toString()
            }
            sb.append(String.format("%s\t x%s", linenType, result.count))
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