package com.github.shingyx.boomswitch

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {
    private lateinit var handler: Handler
    private lateinit var toaster: Toaster
    private lateinit var adapter: BluetoothDeviceAdapter
    private lateinit var bluetoothStateReceiver: BroadcastReceiver

    private val bluetoothOffAlertDialog by lazy {
        AlertDialog.Builder(this)
            .setMessage("Bluetooth is turned off. Please turn Bluetooth on then come back to this app.")
            .setPositiveButton("OK", null)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Preferences.initialize(this)
        handler = Handler()
        toaster = Toaster(this, handler)
        adapter = BluetoothDeviceAdapter()
        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    updateBluetoothDevices()
                }
            }
        }

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.v(TAG, "onNothingSelected")
                Preferences.bluetoothDeviceInfo = null
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.v(TAG, "onItemSelected $position")
                Preferences.bluetoothDeviceInfo = adapter.getItem(position)
            }
        }

        button.setOnClickListener {
            button.isEnabled = false
            BoomClient.switchPower(this) { toaster.show(it) }
                .whenComplete { _, _ ->
                    // Delay to enable the button just as the toast appears
                    handler.postDelayed({
                        button.isEnabled = true
                    }, 250)
                }
        }

        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothDevices()
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothStateReceiver)
        super.onDestroy()
    }

    private fun updateBluetoothDevices() {
        var devices = BluetoothAdapter.getDefaultAdapter()
            ?.takeIf { it.isEnabled }
            ?.bondedDevices
            ?.map { BluetoothDeviceInfo(it) }
            ?.sorted()

        if (devices != null) {
            bluetoothOffAlertDialog.hide()
        } else {
            bluetoothOffAlertDialog.show()
            devices = emptyList()
        }
        // TODO reselect current device
        adapter.updateItems(devices)
    }

    private inner class BluetoothDeviceAdapter : BaseAdapter() {
        private var devices = emptyList<BluetoothDeviceInfo>()

        fun updateItems(items: List<BluetoothDeviceInfo>) {
            devices = items
            notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView
                ?: layoutInflater.inflate(R.layout.spinner_bluetooth_device_info, parent, false)

            val deviceInfo = devices[position]
            view.findViewById<TextView>(R.id.device_name).text = deviceInfo.name
            view.findViewById<TextView>(R.id.device_address).text = deviceInfo.address
            return view
        }

        override fun getItem(position: Int): BluetoothDeviceInfo {
            return devices[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return devices.size
        }
    }
}
