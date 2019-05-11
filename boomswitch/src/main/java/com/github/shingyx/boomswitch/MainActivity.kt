package com.github.shingyx.boomswitch

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {
    private lateinit var handler: Handler
    private lateinit var toaster: Toaster
    private lateinit var adapter: BluetoothDeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Preferences.initialize(this)
        handler = Handler()
        toaster = Toaster(this, handler)
        adapter = BluetoothDeviceAdapter(this)

        // TODO add reloading
        val devices = BluetoothAdapter.getDefaultAdapter()
            ?.takeIf { it.isEnabled }
            ?.bondedDevices
            ?.map { BluetoothDeviceInfo(it) }
            ?: ArrayList()
        adapter.addAll(devices)

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.v(TAG, "onNothingSelected")
                Preferences.bluetoothDeviceInfo = null
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.v(TAG, "onItemSelected $position")
                Preferences.bluetoothDeviceInfo = devices[position]
            }
        }

        button.setOnClickListener {
            button.isEnabled = false
            BoomClient.switchPower(this) { toaster.show(it) }
                .whenComplete { _, _ ->
                    // time re-enabling button with toast animation
                    handler.postDelayed({
                        button.isEnabled = true
                    }, 250)
                }
        }
    }
}

private class BluetoothDeviceAdapter(private val activity: MainActivity) :
    ArrayAdapter<BluetoothDeviceInfo>(activity, android.R.layout.simple_spinner_item) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        android.R.layout.simple_spinner_dropdown_item
        val view = convertView
            ?: LayoutInflater.from(activity).inflate(R.layout.spinner_bluetooth_device_info, parent, false)

        val deviceInfo = getItem(position)!!
        view.findViewById<TextView>(R.id.device_name).text = deviceInfo.name
        view.findViewById<TextView>(R.id.device_address).text = deviceInfo.address
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}
