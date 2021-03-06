package com.hgkefang.transport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.blankj.utilcode.util.SPUtils
import com.bronze.kutil.httpPost
import com.google.gson.Gson
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.ObjectResult
import com.hgkefang.transport.net.API_LOGIN
import com.hgkefang.transport.util.SecretUtil
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.toast

/**
 * Create by admin on 2018/9/7
 * 登录
 */
class LoginActivity : BaseActivity(), View.OnClickListener {

    private val spUtils: SPUtils by lazy{
        SPUtils.getInstance(Activity.MODE_PRIVATE)
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_login
    }

    override fun initialize(savedInstanceState: Bundle?) {
        hasNeedFitWindow = true

//        if (!TextUtils.isEmpty(MyApplication.token)) {
//            startActivity(Intent(this, ScanningActivity::class.java))
//            finish()
//            return
//        }
        if (!spUtils.getString("userName").isNullOrEmpty() &&
                !spUtils.getString("password").isNullOrEmpty()) {
            etAccount.setText(spUtils.getString("userName"))
            etPassword.setText(spUtils.getString("password"))
            if (!MyApplication.token.isNullOrEmpty())
                doLogin()
        }

        tvLogin.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tvLogin -> doLogin()
        }
    }

    private fun doLogin() {
        if (etAccount.text.toString().isEmpty() or etPassword.text.toString().isEmpty()){
            toast("请输入账号和密码")
            return
        }
        showLoadingDialog()
        val params = LinkedHashMap<String, Any>()
        params["account"] = etAccount.text.toString()
        params["password"] = SecretUtil.get32MD5Str(etPassword.text.toString())
        params["sign"] = SecretUtil.get32MD5Str(Gson().toJson(params)).toUpperCase()
        API_LOGIN.httpPost(getRequestParams(Gson().toJson(params))) { statusCode, body ->
            Log.i("doLogin", body)
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
                if (it.errMsg.code != 200) {
                    toast(it.message)
                    return@httpPost
                }
                MyApplication.token = it.retData.token
                MyApplication.name = it.retData.name
                spUtils.put("token", it.retData.token)
                spUtils.put("name", it.retData.name)
                spUtils.put("userName", etAccount.text.toString())
                spUtils.put("password", etPassword.text.toString())
                startActivity(Intent(this, ScanningActivity::class.java))
                finish()
            }
        }
    }
}