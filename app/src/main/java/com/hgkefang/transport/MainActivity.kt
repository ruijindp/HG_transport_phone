package com.hgkefang.transport

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.blankj.utilcode.util.*
import com.bronze.kutil.httpPost
import com.daimajia.numberprogressbar.NumberProgressBar
import com.google.gson.Gson
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.ObjectResult
import com.hgkefang.transport.entity.Version
import com.hgkefang.transport.net.API_CHECK_EXPIRE
import com.hgkefang.transport.net.API_UPDATE
import com.hgkefang.transport.util.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_title.*
import org.jetbrains.anko.toast
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class MainActivity : BaseActivity(), View.OnClickListener {

    private val CONN_STATE_DISCONNECT = 0x007
    private val PRINTER_COMMAND_ERROR = 0x008
    private val ACTION_QUERY_PRINTER_STATE = "action_query_printer_state"
    private val CONN_STATE_FAILED = CONN_STATE_DISCONNECT shl 2
    private val CONN_PRINTER = 0x12
    private val MESSAGE_UPDATE_PARAMETER = 0x009
    private val REQUEST_PERMISSION = 0x123

    private val id = 1
    private var threadPool: ThreadPool? = null
    private val spUtils = SPUtils.getInstance(Activity.MODE_PRIVATE)
    private var downloadId: Long = 0
    private lateinit var downloadReceiver: DownloadReceiver
    private var downloadManagerUtil: DownloadManagerUtil by Delegates.notNull()
    private lateinit var downloadDialog: Dialog

    override fun getLayoutID(): Int {
        return R.layout.activity_main
    }

    override fun initialize(savedInstanceState: Bundle?) {
        ivPageBack.visibility = View.GONE
        ivScanning.visibility = View.VISIBLE
        flSendLinen.setOnClickListener(this)
        flPickLinen.setOnClickListener(this)
        flPollution.setOnClickListener(this)
        flRewashLinen.setOnClickListener(this)
        ivPrinter.setOnClickListener(this)
        ivScanning.setOnClickListener(this)
        tvSignOut.setOnClickListener(this)

        downloadManagerUtil = DownloadManagerUtil(this@MainActivity)
        checkIsMaturity()
        tvPageTitle.text = MyApplication.retData?.tradition_hotel_name
        connectBle()

//        checkVersion()

        downloadReceiver = DownloadReceiver(this@MainActivity)
        val intentFilter = IntentFilter()
        intentFilter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        checkVersion()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_PERMISSION) {
            val file = FileUtils.getFileByPath(apkPath?.replace("file://", ""))
            val apkUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val install = Intent(Intent.ACTION_VIEW)
            install.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            install.setDataAndType(apkUri, "application/vnd.android.package-archive")
            startActivity(install)
        } else {
            toast("未允许未知来源，安装失败")
        }
    }

    private fun connectBle() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            return
        }
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
        }
    }

    //检查是否过期
    private fun checkIsMaturity() {
        showLoadingDialog()
        val param = mapOf("token" to MyApplication.token)
        API_CHECK_EXPIRE.httpPost(getRequestParams(Gson().toJson(param))) { statusCode, body ->
            Log.i("response_check", body)
            dismissDialog()
            if (statusCode != 200) {
                toast("网络错误：$statusCode")
                return@httpPost
            }
            if (isJsonArrayType(body)) {
                toast(getJsonMessage(body))
                return@httpPost
            }
            Gson().fromJson<ObjectResult>(body, ObjectResult::class.java).let {
                if (it.errMsg.code == 301) {
                    tokenInvalid()
                    return@httpPost
                }
                if (it.errMsg.code != 200) {
                    toast(it.message)
                    when (it.message) {
                        "当前洗涤厂使用期限已到" -> {
                            val intent = Intent(this@MainActivity, MaturityActivity::class.java)
                            intent.putExtra("time", it.retData.time)
                            startActivity(intent)
                            finish()
                        }
                        "当前洗涤厂还有即将到期" -> {
                            showMaturityDialog(it.retData.time)
                        }
                    }
                    return@httpPost
                }
                toast(it.message)
            }
        }
    }

    //即将到期dialog
    private fun showMaturityDialog(time: String) {
        val maturityDialog = Dialog(this@MainActivity, R.style.AppTheme_Dialog)
        maturityDialog.setContentView(R.layout.dialog_maturity)
        maturityDialog.setCancelable(false)
        maturityDialog.show()
        val beginDate = TimeUtil.strTime2Date(time, "yyyy-MM-dd")
        val endTime = TimeUtils.millis2String(System.currentTimeMillis(), SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE))
        val tvDate = maturityDialog.findViewById<TextView>(R.id.tvDate)
        val spannableString = SpannableString(String.format(getString(R.string.maturity_date), TimeUtil.getTimeSpan(beginDate, endTime).toString()))
        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(this@MainActivity, R.color.colorAccent))
        spannableString.setSpan(colorSpan, spannableString.indexOf('有') + 1, spannableString.indexOf('，'), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        tvDate.text = spannableString
        maturityDialog.findViewById<ImageView>(R.id.ivClose).setOnClickListener {
            if (maturityDialog.isShowing) {
                maturityDialog.dismiss()
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.flSendLinen ->
                startActivity(Intent(this@MainActivity, LinenTypeActivity::class.java).putExtra("pageValue", 1))
            R.id.flPickLinen ->
                startActivity(Intent(this@MainActivity, LinenTypeActivity::class.java).putExtra("pageValue", 2))
            R.id.flPollution ->
                startActivity(Intent(this@MainActivity, LinenTypeActivity::class.java).putExtra("pageValue", 3))
            R.id.flRewashLinen ->
                startActivity(Intent(this@MainActivity, LinenTypeActivity::class.java).putExtra("pageValue", 4))
            R.id.ivPrinter ->
                startActivity(Intent(this@MainActivity, HistoryOrderActivity::class.java))
            R.id.ivScanning -> {
                val intent = Intent(this@MainActivity, ScanningActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
            R.id.tvSignOut -> {
                MyApplication.token = null
                SPUtils.getInstance(Activity.MODE_PRIVATE).remove("token")
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    private fun checkVersion() {
        val params = mapOf("token" to MyApplication.token)
        API_UPDATE.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("response_update", body)
            if (statusCode != 200) {
                return@httpPost
            }
            if (isJsonArrayType(body)) {
                toast(getJsonMessage(body))
                return@httpPost
            }
            Gson().fromJson<ObjectResult>(body, ObjectResult::class.java).let {
                if (it.errMsg.code == 301) {
                    tokenInvalid()
                    return@httpPost
                }
                if (it.errMsg.code != 200) {
                    toast(it.message)
                    return@httpPost
                }
                if (it.retData.app_version.code.toInt() > AppUtils.getAppVersionCode()) {
                    showUpDateDialog(it.retData.app_version)
                }
            }
        }
    }

    private fun showUpDateDialog(version: Version) {
        MaterialDialog.Builder(this)
                .title(R.string.new_version)
                .theme(Theme.LIGHT)
                .cancelable(false)
                .content(String.format("%s%s", version.content, getString(R.string.update)))
                .positiveText(android.R.string.ok)
                .positiveColor(ContextCompat.getColor(this, R.color.colorAccent))
                .onPositive { _, _ ->
                    if (downloadId != 0L) {
                        downloadManagerUtil.clearCurrentTask(downloadId)
                    }
                    val url = URLDecoder.decode(version.url, "UTF-8")
                    downloadId = downloadManagerUtil.download(url, "transport.apk", "下载中，请保持最新版本")
                    showDownloadDialog()
                }.show()
    }

    private fun showDownloadDialog() {
        downloadDialog = MaterialDialog.Builder(this)
                .theme(Theme.LIGHT)
                .title(R.string.downloading)
                .cancelable(false)
                .customView(R.layout.dialog_download, false)
                .show()
        val numberProgressBar = downloadDialog.findViewById(R.id.numberProgressBar) as NumberProgressBar
        downloadManagerUtil.setOnProgressListener(object : DownloadManagerUtil.OnProgressListener {
            override fun onProgress(fraction: Float) {
                numberProgressBar.progress = (fraction * 100).toInt()
            }
        })
    }

    fun dismissDownloadDialog() {
        if (downloadDialog.isShowing) {
            downloadDialog.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter()
        filter.addAction(ACTION_QUERY_PRINTER_STATE)
        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        DeviceConnFactoryManager.closeAllPort()
        if (threadPool != null) {
            threadPool!!.stopThreadPool()
        }
        unregisterReceiver(downloadReceiver)
        downloadManagerUtil.unregisterContentObserver()
    }

    fun closeContentScheduled() {
        downloadManagerUtil.close()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                    mHandler.obtainMessage(CONN_STATE_DISCONNECT).sendToTarget()
                DeviceConnFactoryManager.ACTION_CONN_STATE -> {
                    val state = intent.getIntExtra(DeviceConnFactoryManager.STATE, -1)
                    val deviceId = intent.getIntExtra(DeviceConnFactoryManager.DEVICE_ID, -1)
                    when (state) {
                        DeviceConnFactoryManager.CONN_STATE_DISCONNECT -> {
                            if (id == deviceId) {
                                ToastUtils.showShort("打印机断开")
                                spUtils.remove("macAddress")
                                MyApplication.hasConnectPrinter = false
                            }
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTING -> {
                            ToastUtils.showShort("打印机连接中...")
                            MyApplication.hasConnectPrinter = false
                        }
                        DeviceConnFactoryManager.CONN_STATE_CONNECTED -> {
                            ToastUtils.showShort("打印机已连接")
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
            }
        }
    }

    private var apkPath: String? = null
    fun installApk(apkPath: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val haveInstallPermission = packageManager.canRequestPackageInstalls()
            if (!haveInstallPermission) {
                this.apkPath = apkPath
                val packageURI = Uri.parse("package:$packageName")
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI)
                startActivityForResult(intent, REQUEST_PERMISSION)
            } else {
                val file = FileUtils.getFileByPath(apkPath.replace("file://", ""))
                val apkUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                val install = Intent(Intent.ACTION_VIEW)
                install.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                install.setDataAndType(apkUri, "application/vnd.android.package-archive")
                startActivity(install)
            }
        } else if (Build.VERSION.SDK_INT >= 24) {
            val file = FileUtils.getFileByPath(apkPath.replace("file://", ""))
            val apkUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val install = Intent(Intent.ACTION_VIEW)
            install.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            install.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            install.setDataAndType(apkUri, "application/vnd.android.package-archive")
            startActivity(install)
        } else {
            val install = Intent(Intent.ACTION_VIEW)
            install.setDataAndType(Uri.parse(apkPath), "application/vnd.android.package-archive")
            install.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(install)
        }
    }
}
