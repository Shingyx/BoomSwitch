package com.github.shingyx.boomswitch.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.github.shingyx.boomswitch.R
import com.github.shingyx.boomswitch.data.BluetoothDeviceInfo

class BluetoothDeviceAdapter(
    private val activity: Activity,
    private var devices: List<BluetoothDeviceInfo> = emptyList()
) : TypedAdapter<BluetoothDeviceInfo>(), Filterable {
  private val filter = NoFilter()

  fun updateItems(items: List<BluetoothDeviceInfo>) {
    devices = items
    notifyDataSetChanged()
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
    val view =
        convertView
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
