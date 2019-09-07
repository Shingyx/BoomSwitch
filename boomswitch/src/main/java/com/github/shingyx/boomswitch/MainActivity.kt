package com.github.shingyx.boomswitch

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*

private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {
    private lateinit var handler: Handler
    private lateinit var toaster: Toaster
    private lateinit var adapter: BluetoothDeviceAdapter
    private lateinit var bluetoothStateReceiver: BroadcastReceiver

    private val bluetoothOffAlertDialog by lazy {
        MaterialAlertDialogBuilder(this)
            .setMessage("Bluetooth is turned off. Please turn Bluetooth on then come back to this app.")
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Preferences.initialize(this)
        handler = Handler()
        toaster = Toaster(this, handler)
        adapter = BluetoothDeviceAdapter(this)
        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    updateBluetoothDevices()
                }
            }
        }

        select_speaker.setAdapter(adapter)
        select_speaker.setText(Preferences.bluetoothDeviceInfo.toString())
        select_speaker.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Log.v(TAG, "onItemClick $position")
            Preferences.bluetoothDeviceInfo = adapter.getItem(position)
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

        registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
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
        val bondedDevices = BluetoothAdapter.getDefaultAdapter()
            ?.takeIf { it.isEnabled }
            ?.bondedDevices

        var devicesInfo = bondedDevices?.map { BluetoothDeviceInfo(it) }?.sorted()

        if (devicesInfo != null) {
            bluetoothOffAlertDialog.hide()
        } else {
            bluetoothOffAlertDialog.show()
            devicesInfo = emptyList()
        }

        adapter.updateItems(devicesInfo)
    }
}

private class BluetoothDeviceAdapter(
    private val activity: Activity
) : BaseAdapter(), Filterable {
    private var devices = emptyList<BluetoothDeviceInfo>()
    private val filter = NoFilter()

    fun updateItems(items: List<BluetoothDeviceInfo>) {
        devices = items
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView
            ?: activity.layoutInflater.inflate(R.layout.dropdown_menu_popup_item, parent, false)
        (view as TextView).text = devices[position].toString()
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

    override fun getFilter(): Filter {
        return filter
    }

    private inner class NoFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults().apply {
                values = devices
                count = devices.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }
    }
}
