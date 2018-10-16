package com.hgkefang.transport

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import kotlinx.android.synthetic.main.activity_bluetooth.*
import kotlinx.android.synthetic.main.view_title.*


/**
 * Create by admin on 2018/9/7
 * 蓝牙
 */
class BluetoothActivity : BaseActivity() {

    private val REQUEST_FINE_LOCATION = 1
    private val REQUEST_ENABLE_BT = 2
    private val REQUEST_CODE_OPEN_GPS = 3
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
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(mFindBlueToothReceiver, filter)

        initBluetooth()
        tvScanning.setOnClickListener {
            it.visibility = View.GONE
            view1.visibility = View.VISIBLE
            view2.visibility = View.VISIBLE
            lnSearchState.visibility = View.VISIBLE
            discoveryDevice()
        }
    }

    private fun initBluetooth() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            ToastUtils.showShort("该设备不支持蓝牙")
        } else {
            if (!mBluetoothAdapter!!.isEnabled) {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_FINE_LOCATION)
                } else {
                    getDeviceList()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != REQUEST_FINE_LOCATION) {
            return
        }
        if (grantResults[0] != -1) {
            getDeviceList()
        } else {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter?.cancelDiscovery()
        }
        unregisterReceiver(mFindBlueToothReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                getDeviceList()
            }
            REQUEST_CODE_OPEN_GPS -> {
                discoveryDevice()
            }
        }
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
                    mNewDevicesArrayAdapter?.add(device.name + "\n" + device.address)
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                if (mNewDevicesArrayAdapter!!.count == 0) {
                    val noDevices = resources.getText(R.string.none_bluetooth_device_found).toString()
                    mNewDevicesArrayAdapter?.add(noDevices)
                }
            }
            mNewDevicesArrayAdapter?.notifyDataSetChanged()
        }
    }

    private fun discoveryDevice() {
        if (mBluetoothAdapter!!.isDiscovering) {
            mBluetoothAdapter?.cancelDiscovery()
        }
        mBluetoothAdapter?.startDiscovery()
    }
}