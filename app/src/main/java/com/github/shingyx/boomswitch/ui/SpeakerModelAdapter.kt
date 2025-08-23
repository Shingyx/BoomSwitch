package com.github.shingyx.boomswitch.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.github.shingyx.boomswitch.R

class SpeakerModelAdapter(private val activity: Activity) :
  TypedAdapter<SpeakerModel>(), Filterable {
  private val filter = NoFilter()
  private val speakerModels = SpeakerModel.entries

  override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
    val view =
      convertView
        ?: activity.layoutInflater.inflate(R.layout.dropdown_menu_popup_item, parent, false)
    (view as TextView).setText(speakerModels[position].modelStringResId)
    return view
  }

  override fun getItem(position: Int): SpeakerModel {
    return speakerModels[position]
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getCount(): Int {
    return speakerModels.size
  }

  override fun getFilter(): Filter {
    return filter
  }

  private inner class NoFilter : Filter() {
    override fun performFiltering(constraint: CharSequence?): FilterResults {
      return FilterResults().apply {
        values = speakerModels
        count = speakerModels.size
      }
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
      notifyDataSetChanged()
    }
  }
}
