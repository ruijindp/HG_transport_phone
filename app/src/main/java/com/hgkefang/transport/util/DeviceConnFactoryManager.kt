package com.hgkefang.transport.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.gprinter.io.*
import com.hgkefang.transport.R
import com.hgkefang.transport.app.MyApplication
import java.io.IOException
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.experimental.and

/**
 * Create by admin on 2018/6/11
 */
class DeviceConnFactoryManager private constructor(build: Build) {
    private var mPort: PortManager? = null

    /**
     * 获取端口连接方式
     *
     * @return
     */
    private val connMethod: CONN_METHOD?

    /**
     * 获取连接网口的IP
     *
     * @return
     */
    private val ip: String?

    /**
     * 获取连接网口端口号
     *
     * @return
     */
    private val port: Int

    /**
     * 获取连接蓝牙的物理地址
     *
     * @return
     */
    private val macAddress: String?

    private val mUsbDevice: UsbDevice?

    private val mContext: Context?

    /**
     * 获取串口号
     *
     * @return
     */
    private val serialPortPath: String?

    /**
     * 获取波特率
     *
     * @return
     */
    var baudrate: Int = 0

    private var pId: Int = 0

    /**
     * 获取端口打开状态（true 打开，false 未打开）
     *
     * @return
     */
    var connState: Boolean = false
        private set
    /**
     * ESC查询打印机实时状态指令
     */
    private val esc = byteArrayOf(0x10, 0x04, 0x02)

    /**
     * TSC查询打印机状态指令
     */
    private val tsc = byteArrayOf(0x1b, '!'.toByte(), '?'.toByte())
    private var sendCommand: ByteArray? = null
    /**
     * 判断打印机所使用指令是否是ESC指令
     */
    private var currentPrinterCommand: PrinterCommand? = null
    private var reader: PrinterReader? = null

    @SuppressLint("HandlerLeak")
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                READ_DATA -> {
                    val cnt = msg.data.getInt(READ_DATA_CNT)
                    val buffer = msg.data.getByteArray(READ_BUFFER_ARRAY) ?: return
                    //这里只对查询状态返回值做处理，其它返回值可参考编程手册来解析
                    val result = judgeResponseType(buffer[0])
                    var status = MyApplication.context!!.getString(R.string.str_printer_conn_normal)
                    if (sendCommand!!.contentEquals(esc)) {
                        //设置当前打印机模式为ESC模式
                        if (currentPrinterCommand == null) {
                            currentPrinterCommand = PrinterCommand.ESC
                            sendStateBroadcast(CONN_STATE_CONNECTED)
                        } else {//查询打印机状态
                            if (result == 0) {//打印机状态查询
                                val intent = Intent(ACTION_QUERY_PRINTER_STATE)
                                intent.putExtra(DEVICE_ID, pId)
                                MyApplication.context!!.sendBroadcast(intent)
                            } else if (result == 1) {//查询打印机实时状态
                                if (buffer[0] and ESC_STATE_PAPER_ERR.toByte() > 0) {
                                    status += " " + MyApplication.context!!.getString(R.string.str_printer_out_of_paper)
                                }
                                if (buffer[0] and ESC_STATE_COVER_OPEN.toByte() > 0) {
                                    status += " " + MyApplication.context!!.getString(R.string.str_printer_open_cover)
                                }
                                if (buffer[0] and ESC_STATE_ERR_OCCURS.toByte() > 0) {
                                    status += " " + MyApplication.context!!.getString(R.string.str_printer_error)
                                }
                                ToastUtils.showShort(status)
                            }
                        }
                    } else if (sendCommand!!.contentEquals(tsc)) {
                        //设置当前打印机模式为TSC模式
                        if (currentPrinterCommand == null) {
                            currentPrinterCommand = PrinterCommand.TSC
                            sendStateBroadcast(CONN_STATE_CONNECTED)
                        } else {
                            if (cnt == 1) {//查询打印机实时状态
                                if (buffer[0] and TSC_STATE_PAPER_ERR.toByte() > 0) {//缺纸
                                    status += " " + MyApplication.context!!.getString(R.string.str_printer_out_of_paper)
                                }
                                if (buffer[0] and TSC_STATE_COVER_OPEN.toByte() > 0) {//开盖
                                    status += " " + MyApplication.context!!.getString(R.string.str_printer_open_cover)
                                }
                                if (buffer[0] and TSC_STATE_ERR_OCCURS.toByte() > 0) {//打印机报错
                                    status += " " + MyApplication.context!!.getString(R.string.str_printer_error)
                                }
                                ToastUtils.showShort(status)
                            } else {//打印机状态查询
                                val intent = Intent(ACTION_QUERY_PRINTER_STATE)
                                intent.putExtra(DEVICE_ID, pId)
                                MyApplication.context!!.sendBroadcast(intent)
                            }
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    private val usbStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                ACTION_USB_DEVICE_DETACHED -> sendStateBroadcast(CONN_STATE_DISCONNECT)
            }
        }
    }

    enum class CONN_METHOD(val s: String) {
        //蓝牙连接
        BLUETOOTH("BLUETOOTH"),
        //USB连接
        USB("USB"),
        //wifi连接
        WIFI("WIFI"),
        //串口连接
        SERIAL_PORT("SERIAL_PORT");

        override fun toString(): String {
            return this.s
        }
    }

    /**
     * 打开端口
     *
     * @return
     */
    fun openPort() {
        deviceConnFactoryManagers[pId]!!.connState = false
        sendStateBroadcast(CONN_STATE_CONNECTING)
        when (deviceConnFactoryManagers[pId]!!.connMethod) {
            DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH -> {
                println("id -> $pId")
                mPort = BluetoothPort(macAddress)
                connState = deviceConnFactoryManagers[pId]!!.mPort!!.openPort()
            }
            DeviceConnFactoryManager.CONN_METHOD.USB -> {
                mPort = UsbPort(mContext!!, mUsbDevice)
                connState = mPort!!.openPort()
                if (connState) {
                    val filter = IntentFilter(ACTION_USB_DEVICE_DETACHED)
                    mContext.registerReceiver(usbStateReceiver, filter)
                }
            }
            DeviceConnFactoryManager.CONN_METHOD.WIFI -> {
                mPort = EthernetPort(ip, port)
                connState = mPort!!.openPort()
            }
            DeviceConnFactoryManager.CONN_METHOD.SERIAL_PORT -> {
                mPort = SerialPort(serialPortPath, baudrate, 0)
                connState = mPort!!.openPort()
            }
            else -> {
            }
        }
        //端口打开成功后，检查连接打印机所使用的打印机指令ESC、TSC
        if (connState) {
            queryCommand()
        } else {
            sendStateBroadcast(CONN_STATE_FAILED)
        }
    }

    /**
     * 查询当前连接打印机所使用打印机指令（ESC（EscCommand.java）、TSC（LabelCommand.java））
     */
    private fun queryCommand() {
        //开启读取打印机返回数据线程
        reader = PrinterReader()
        reader!!.start()
        //查询打印机所使用指令
        queryPrinterCommand()
    }

    /**
     * 获取连接的USB设备信息
     *
     * @return
     */
    fun usbDevice(): UsbDevice? {
        return mUsbDevice
    }

    /**
     * 关闭端口
     */
    fun closePort(id: Int) {
        if (this.mPort != null) {
            println("id -> $id")
            this.mPort!!.closePort()
            connState = false
            currentPrinterCommand = null
        }
        sendStateBroadcast(CONN_STATE_DISCONNECT)
    }

    init {
        this.connMethod = build.connMethod
        this.macAddress = build.macAddress
        this.port = build.port
        this.ip = build.ip
        this.mUsbDevice = build.usbDevice
        this.mContext = build.context
        this.serialPortPath = build.serialPortPath
        this.baudrate = build.baudrate
        this.pId = build.id
        deviceConnFactoryManagers[pId] = this
    }

    /**
     * 获取当前打印机指令
     *
     * @return PrinterCommand
     */
    fun getCurrentPrinterCommand(): PrinterCommand? {
        return deviceConnFactoryManagers[pId]!!.currentPrinterCommand
    }

    class Build {
        internal var ip: String? = null
        internal var macAddress: String? = null
        internal var usbDevice: UsbDevice? = null
        internal var port: Int = 0
        internal var connMethod: CONN_METHOD? = null
        internal var context: Context? = null
        internal var serialPortPath: String? = null
        internal var baudrate: Int = 0
        internal var id: Int = 0

        fun setIp(ip: String): DeviceConnFactoryManager.Build {
            this.ip = ip
            return this
        }

        fun setMacAddress(macAddress: String): DeviceConnFactoryManager.Build {
            this.macAddress = macAddress
            return this
        }

        fun setUsbDevice(usbDevice: UsbDevice): DeviceConnFactoryManager.Build {
            this.usbDevice = usbDevice
            return this
        }

        fun setPort(port: Int): DeviceConnFactoryManager.Build {
            this.port = port
            return this
        }

        fun setConnMethod(connMethod: CONN_METHOD): DeviceConnFactoryManager.Build {
            this.connMethod = connMethod
            return this
        }

        fun setContext(context: Context): DeviceConnFactoryManager.Build {
            this.context = context
            return this
        }

        fun setId(id: Int): DeviceConnFactoryManager.Build {
            this.id = id
            return this
        }

        fun setSerialPort(serialPortPath: String): DeviceConnFactoryManager.Build {
            this.serialPortPath = serialPortPath
            return this
        }

        fun setBaudrate(baudrate: Int): DeviceConnFactoryManager.Build {
            this.baudrate = baudrate
            return this
        }

        fun build(): DeviceConnFactoryManager {
            return DeviceConnFactoryManager(this)
        }
    }

    fun sendDataImmediately(data: Vector<Byte>) {
        if (this.mPort == null) {
            return
        }
        try {
            Log.e(TAG, "data -> " + String(com.gprinter.utils.Utils.convertVectorByteTobytes(data), charset("gb2312")))
            this.mPort!!.writeDataImmediately(data, 0, data.size)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    fun readDataImmediately(buffer: ByteArray): Int {
        return this.mPort!!.readData(buffer)
    }

    /**
     * 查询打印机当前使用的指令（TSC、ESC）
     */
    private fun queryPrinterCommand() {
        ThreadPool.instantiation.addTask(Runnable {
            //发送ESC查询打印机状态指令
            sendCommand = esc
            val data = Vector<Byte>(esc.size)
            for (i in esc.indices) {
                data.add(esc[i])
            }
            sendDataImmediately(data)
            //开启计时器，隔2000毫秒没有没返回值时发送TSC查询打印机状态指令
            val threadFactoryBuilder = ThreadFactoryBuilder("Timer")
            val scheduledExecutorService = ScheduledThreadPoolExecutor(1, threadFactoryBuilder)
            scheduledExecutorService.schedule(threadFactoryBuilder.newThread {
                if (currentPrinterCommand == null || currentPrinterCommand !== PrinterCommand.ESC) {
                    Log.e(TAG, Thread.currentThread().name)
                    //发送TSC查询打印机状态指令
                    sendCommand = tsc
                    val data = Vector<Byte>(tsc.size)
                    for (i in tsc.indices) {
                        data.add(tsc[i])
                    }
                    sendDataImmediately(data)
                    //开启计时器，隔2000毫秒打印机没有响应者停止读取打印机数据线程并且关闭端口
                    scheduledExecutorService.schedule(threadFactoryBuilder.newThread {
                        if (currentPrinterCommand == null) {
                            if (reader != null) {
                                reader!!.cancel()
                                mPort!!.closePort()
                                connState = false
                                sendStateBroadcast(CONN_STATE_FAILED)
                            }
                        }
                    }, 2000, TimeUnit.MILLISECONDS)
                }
            }, 2000, TimeUnit.MILLISECONDS)
        })
    }

    internal inner class PrinterReader : Thread() {
        private var isRun: Boolean = false

        private val buffer = ByteArray(100)

        init {
            isRun = true
        }

        override fun run() {
            try {
                while (isRun) {
                    //读取打印机返回信息
                    val len = readDataImmediately(buffer)
                    if (len > 0) {
                        val message = Message.obtain()
                        message.what = READ_DATA
                        val bundle = Bundle()
                        bundle.putInt(READ_DATA_CNT, len)
                        bundle.putByteArray(READ_BUFFER_ARRAY, buffer)
                        message.data = bundle
                        mHandler.sendMessage(message)
                    }
                }
            } catch (e: Exception) {
                if (deviceConnFactoryManagers[pId] != null) {
                    closePort(pId)
                }
            }

        }

        fun cancel() {
            isRun = false
        }
    }

    private fun sendStateBroadcast(state: Int) {
        val intent = Intent(ACTION_CONN_STATE)
        intent.putExtra(STATE, state)
        intent.putExtra(DEVICE_ID, pId)
        MyApplication.context!!.sendBroadcast(intent)
    }

    /**
     * 判断是实时状态（10 04 02）还是查询状态（1D 72 01）
     */
    private fun judgeResponseType(r: Byte): Int {
        return (r and FLAG).toInt().shr(4)
    }

    companion object {

        private val TAG = DeviceConnFactoryManager::class.java.simpleName

        val deviceConnFactoryManagers = arrayOfNulls<DeviceConnFactoryManager>(4)

        /**
         * ESC查询打印机实时状态 缺纸状态
         */
        private const val ESC_STATE_PAPER_ERR = 0x20

        /**
         * ESC指令查询打印机实时状态 打印机开盖状态
         */
        private const val ESC_STATE_COVER_OPEN = 0x04

        /**
         * ESC指令查询打印机实时状态 打印机报错状态
         */
        private const val ESC_STATE_ERR_OCCURS = 0x40

        /**
         * TSC指令查询打印机实时状态 打印机缺纸状态
         */
        private const val TSC_STATE_PAPER_ERR = 0x04

        /**
         * TSC指令查询打印机实时状态 打印机开盖状态
         */
        private const val TSC_STATE_COVER_OPEN = 0x01

        /**
         * TSC指令查询打印机实时状态 打印机出错状态
         */
        private const val TSC_STATE_ERR_OCCURS = 0x80
        const val FLAG: Byte = 0x10
        private const val READ_DATA = 10000
        private const val READ_DATA_CNT = "read_data_cnt"
        private const val READ_BUFFER_ARRAY = "read_buffer_array"
        const val ACTION_CONN_STATE = "action_connect_state"
        const val ACTION_QUERY_PRINTER_STATE = "action_query_printer_state"
        const val STATE = "state"
        const val DEVICE_ID = "id"
        const val CONN_STATE_DISCONNECT = 0x90
        const val CONN_STATE_CONNECTING = CONN_STATE_DISCONNECT shl 1
        const val CONN_STATE_FAILED = CONN_STATE_DISCONNECT shl 2
        const val CONN_STATE_CONNECTED = CONN_STATE_DISCONNECT shl 3

        fun closeAllPort() {
            for (deviceConnFactoryManager in deviceConnFactoryManagers) {
                if (deviceConnFactoryManager != null) {
                    Log.e(TAG, "cloaseAllPort() id -> " + deviceConnFactoryManager.pId)
                    deviceConnFactoryManager.closePort(deviceConnFactoryManager.pId)
                    deviceConnFactoryManagers[deviceConnFactoryManager.pId] = null
                }
            }
        }
    }
}
