package com.hgkefang.transport

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import kotlinx.android.synthetic.main.activity_bluetooth.*
import kotlinx.android.synthetic.main.view_title.*
import org.jetbrains.anko.toast

/**
 * Create by admin on 2018/9/7
 * 蓝牙
 */
class BluetoothActivity : BaseActivity() {

    val EXTRA_DEVICE_ADDRESS = "address"
    private val REQUEST_ENABLE_BT = 2
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mNewDevicesArrayAdapter: ArrayAdapter<String>? = null

    override fun getLayoutID(): Int {
        return R.layout.activity_bluetooth
    }

    override fun initialize(savedInstanceState: Bundle?) {
        tvPageTitle.text = getString(R.string.scanning)
        ivPageBack.setOnClickListener {
            if (mBluetoothAdapter != null) {
                mBluetoothAdapter!!.cancelDiscovery()
            }
            finish()
        }

        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mFindBlueToothReceiver, filter)
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(mFindBlueToothReceiver, filter)
        initBluetooth()
        tvScanning.setOnClickListener {
            it.visibility = View.GONE
            discoveryDevice()
        }
    }

    private fun initBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            ToastUtils.showShort("该设备不支持蓝牙")
        } else {
            if (!mBluetoothAdapter!!.isEnabled) {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            } else {
                getDeviceList()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter!!.cancelDiscovery()
        }
        unregisterReceiver(mFindBlueToothReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_ENABLE_BT) {
            return
        }
        if (resultCode != Activity.RESULT_OK) {
            toast("蓝牙不可用")
            return
        }
        getDeviceList()
    }

    private fun getDeviceList() {
        mNewDevicesArrayAdapter = ArrayAdapter(this@BluetoothActivity, R.layout.item_bluetooth)
        lvNewDevices.adapter = mNewDevicesArrayAdapter
        lvNewDevices.onItemClickListener = mDeviceClickListener
    }

    private val mDeviceClickListener = AdapterView.OnItemClickListener { _, v, _, _ ->
        mBluetoothAdapter!!.cancelDiscovery()
        val info = (v as TextView).text.toString()
        val noDevices = resources.getText(R.string.none_paired).toString()
        val noNewDevice = resources.getText(R.string.none_bluetooth_device_found).toString()
        if (info != noDevices && info != noNewDevice) {
            val address = info.substring(info.length - 17)
            val intent = Intent()
            intent.putExtra("address", address)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private val mFindBlueToothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter!!.add(device.name + "\n" + device.address)
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                setProgressBarIndeterminateVisibility(false)
                setTitle(R.string.select_bluetooth_device)
                Log.i("tag", "finish discovery" + mNewDevicesArrayAdapter!!.count)
                if (mNewDevicesArrayAdapter!!.count == 0) {
                    val noDevices = resources.getText(
                            R.string.none_bluetooth_device_found).toString()
                    mNewDevicesArrayAdapter!!.add(noDevices)
                }
            }
        }
    }

    private fun discoveryDevice() {
        setProgressBarIndeterminateVisibility(true)
        setTitle(R.string.scanning)
        if (mBluetoothAdapter!!.isDiscovering) {
            mBluetoothAdapter!!.cancelDiscovery()
        }
        mBluetoothAdapter!!.startDiscovery()
    }
}