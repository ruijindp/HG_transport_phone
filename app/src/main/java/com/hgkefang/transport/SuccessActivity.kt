package com.hgkefang.transport

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.blankj.utilcode.util.TimeUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gprinter.command.EscCommand
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.fragment.HistoryOrderFragment
import com.hgkefang.transport.service.PrinterService
import com.hgkefang.transport.util.DeviceConnFactoryManager
import kotlinx.android.synthetic.main.activity_success.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Create by admin on 2018/9/5
 * 提交成功
 */
class SuccessActivity : BaseActivity() {

    private var linenResult: ArrayList<EvenBusEven>? = null
    private var pageValue: Int = 0
    private lateinit var myService: PrinterService

    override fun getLayoutID(): Int {
        return R.layout.activity_success
    }

    override fun initialize(savedInstanceState: Bundle?) {
        bindService(Intent(this, PrinterService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        pageValue = intent.getIntExtra("pageValue", -1)
        linenResult = Gson().fromJson<EvenBusEven>(intent.getStringExtra("linen"), object : TypeToken<List<EvenBusEven>>() {}.type) as ArrayList<EvenBusEven>
        tvLinenCount.text = String.format(getString(R.string.commit_count), intent.getIntExtra("totalLinen", -1))
        tvBackMain.setOnClickListener {
            val intent = Intent(this@SuccessActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        tvSeeOrder.setOnClickListener {
            LinenTypeActivity.isFinish = true
            startActivity(Intent(this@SuccessActivity, HistoryOrderActivity::class.java))
            finish()
        }
        tvPrinterOrder.setOnClickListener {
            btnReceiptPrint()
        }
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

        esc.addText("下单时间：${TimeUtils.millis2String(System.currentTimeMillis(), SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE) as DateFormat)}\n")

        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER)
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF)
        esc.addText("HG客房管家\n\n")

        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF)
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT)
        esc.addPrintAndLineFeed()
        esc.addText("酒店名称：${MyApplication.retData?.tradition_hotel_name}\n")
        esc.addText("经手人：${MyApplication.name}\n")
        if (!MyApplication.retData?.floor_name.isNullOrEmpty()) {
            esc.addText(String.format("%s%s\n", getString(R.string.category_name_), MyApplication.retData?.floor_name))
        }
        when (pageValue) {
            1 -> esc.addText("布草类型：${getString(R.string.send_linen)}\n")
            2 -> esc.addText("布草类型：${getString(R.string.pick_linen)}\n")
            3 -> esc.addText("布草类型：${getString(R.string.pollution)}\n")
            4 -> esc.addText("布草类型：${getString(R.string.rewash_linen)}\n")
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
        for (result in linenResult!!) {
            val sb = StringBuilder()
            totalLinenCount += result.count
            var linenType = "${result.son.tradition_name}-${result.son.tradition_spec}"
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
        DeviceConnFactoryManager.deviceConnFactoryManagers[PrinterService.id]!!.sendDataImmediately(data)
    }
}