package com.hgkefang.transport.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.text.TextUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ToastUtils
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.util.DeviceConnFactoryManager
import com.hgkefang.transport.util.PrinterCommand
import com.hgkefang.transport.util.ThreadPool
import org.jetbrains.anko.doAsync

/**
 * Create by admin on 2018/10/15
 */
class PrinterService : Service() {

    private val CONN_STATE_DISCONNECT = 0x007
    private val PRINTER_COMMAND_ERROR = 0x008
    private val MESSAGE_PUT = 0x002
    private val CONN_PRINTER = 0x12
    private val MESSAGE_UPDATE_PARAMETER = 0x009
    private val CONN_STATE_FAILED = CONN_STATE_DISCONNECT shl 2
    var id = 0
    private var threadPool: ThreadPool? = null
    private var retData: RetData? = null

    companion object {
        const val id = 0
    }

    override fun onCreate() {
        super.onCreate()
        connectBlePrinter()

        val filter = IntentFilter()
        filter.addAction("action_query_printer_state")
        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE)
        registerReceiver(receiver, filter)
    }

    fun hasConnectPrinter(): Boolean {
        return DeviceConnFactoryManager.deviceConnFactoryManagers[id] != null && DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.connState
    }

    fun disconnectPrinter() {
        handler.obtainMessage(CONN_STATE_DISCONNECT).sendToTarget()
    }

    private fun connectBlePrinter() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            return
        }
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            return
        }
        if (SPUtils.getInstance().getString("macAddress", "").isNullOrEmpty()) {
            return
        }
        doAsync {
            Looper.prepare()
            Handler().post {
                DeviceConnFactoryManager.Build()
                        .setId(id)
                        .setConnMethod(DeviceConnFactoryManager.ConnectType.BLUETOOTH)
                        .setMacAddress(SPUtils.getInstance().getString("macAddress", ""))
                        .build()
                DeviceConnFactoryManager.deviceConnFactoryManagers[id]?.openPort()
                if (!DeviceConnFactoryManager.deviceConnFactoryManagers[id]!!.connState) {
                    ToastUtils.showShort("未找到打印机")
                }
            }
            Looper.loop()
        }
    }

    fun doPrinter() {
        threadPool = ThreadPool.instantiation
        threadPool?.addTask(Runnable {
            if (DeviceConnFactoryManager.deviceConnFactoryManagers[id]?.getCurrentPrinterCommand() == PrinterCommand.ESC) {
                if (notifyPrinterListener != null) {
                    notifyPrinterListener?.onNotifyPrinter(1)
                }
            } else {
                handler.obtainMessage(PRINTER_COMMAND_ERROR).sendToTarget()
            }
        })
    }

    private var macAddress: String? = null

    fun onActivityResult(macAddress: String) {
        this.macAddress = macAddress
        DeviceConnFactoryManager.Build().setId(id)
                .setConnMethod(DeviceConnFactoryManager.ConnectType.BLUETOOTH)
                .setMacAddress(macAddress)
                .build()
        DeviceConnFactoryManager.deviceConnFactoryManagers[id]?.openPort()
    }

    inner class MyBinder : Binder() {
        fun getService(): PrinterService {
            return this@PrinterService
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return MyBinder()
    }

    interface NotifyPrinterListener {
        fun onNotifyPrinter(state: Int)
    }

    private var notifyPrinterListener: NotifyPrinterListener? = null

    fun setNotifyPrinterListener(notifyPrinterListener: NotifyPrinterListener) {
        this.notifyPrinterListener = notifyPrinterListener
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handler.obtainMessage(CONN_STATE_DISCONNECT).sendToTarget()
                DeviceConnFactoryManager.ACTION_CONN_STATE -> {
                    val state = intent.getIntExtra(DeviceConnFactoryManager.STATE, -1)
                    val deviceId = intent.getIntExtra(DeviceConnFactoryManager.DEVICE_ID, -1)
                    when (state) {
                        DeviceConnFactoryManager.CONN_STATE_DISCONNECT -> if (id == deviceId) {
                            ToastUtils.showShort("打印机断开")
                            if (notifyPrinterListener != null) {
                                notifyPrinterListener?.onNotifyPrinter(2)
                            }
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTING -> {
                            ToastUtils.showShort("打印机连接中...")
                            if (notifyPrinterListener != null) {
                                notifyPrinterListener?.onNotifyPrinter(3)
                            }
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTED -> {
                            ToastUtils.showShort("打印机已连接")
                            handler.obtainMessage(MESSAGE_PUT).sendToTarget()
                            if (notifyPrinterListener != null) {
                                notifyPrinterListener?.onNotifyPrinter(4)
                            }
                        }
                        CONN_STATE_FAILED -> {
                            ToastUtils.showShort("打印机连接失败")
                            if (notifyPrinterListener != null) {
                                notifyPrinterListener?.onNotifyPrinter(5)
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                CONN_STATE_DISCONNECT -> if (DeviceConnFactoryManager.deviceConnFactoryManagers[id] != null) {
                    DeviceConnFactoryManager.deviceConnFactoryManagers[id]?.closePort(id)
                }
                PRINTER_COMMAND_ERROR -> ToastUtils.showShort("请选择正确的打印机指令")
                CONN_PRINTER -> ToastUtils.showShort("请先连接打印机")
                MESSAGE_UPDATE_PARAMETER -> {
                    val strIp = msg.data.getString("Ip")
                    val strPort = msg.data.getString("Port")
                    //初始化端口信息
                    DeviceConnFactoryManager.Build()
                            //设置端口连接方式
                            .setConnMethod(DeviceConnFactoryManager.ConnectType.WIFI)
                            //设置端口IP地址
                            .setIp(strIp)
                            //设置端口ID（主要用于连接多设备）
                            .setId(id)
                            //设置连接的热点端口号
                            .setPort(Integer.parseInt(strPort))
                            .build()
                    threadPool = ThreadPool.instantiation
                    threadPool?.addTask(Runnable { DeviceConnFactoryManager.deviceConnFactoryManagers[id]?.openPort() })
                }
                MESSAGE_PUT -> if (TextUtils.isEmpty(SPUtils.getInstance().getString("macAddress", ""))) {
                    SPUtils.getInstance().put("macAddress", macAddress)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        DeviceConnFactoryManager.closeAllPort()
        if (threadPool != null) {
            threadPool?.stopThreadPool()
        }
    }

    fun setRetData(retData: RetData) {
        this.retData = retData
    }

    fun getRetData():RetData?{
        return retData
    }
}
