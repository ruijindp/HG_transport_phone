package com.hgkefang.transport

import android.app.Dialog
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.TimeUtils
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.ObjectResult
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.net.API_CHECK_EXPIRE
import com.hgkefang.transport.util.TimeUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_title.*
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity(), View.OnClickListener {

    override fun getLayoutID(): Int {
        return R.layout.activity_main
    }

    override fun initialize() {
        val retDate = intent.getSerializableExtra("retData") as RetData
        ivPageBack.visibility = View.GONE
        ivScanning.visibility = View.VISIBLE
        flSendLinen.setOnClickListener(this)
        flPickLinen.setOnClickListener(this)
        flPollution.setOnClickListener(this)
        flRewashLinen.setOnClickListener(this)
        ivPrinter.setOnClickListener(this)
        ivScanning.setOnClickListener(this)
//        checkIsMaturity()
        tvPageTitle.text  = retDate.tradition_hotel_name
    }

    //检查是否过期
    private fun checkIsMaturity() {
        showLoadingDialog()
        val param = LinkedHashMap<String, Any?>()
        param["token"] = MyApplication.token
        API_CHECK_EXPIRE.httpPost(getRequestParams(Gson().toJson(param))) { statusCode, body ->
            Log.i("response_check", body)
            dismissDialog()
            if (statusCode != 200) {
                toast("网络错误：$statusCode")
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
//                MyApplication.token = null
//                SPUtils.getInstance(Activity.MODE_PRIVATE).clear()
                val intent = Intent(this@MainActivity, HotelActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }

        }
    }
}
