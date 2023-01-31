package com.github.shingyx.boomswitch.ui

import android.widget.AdapterView

fun <T> adapterOnItemClick(
    adapter: TypedAdapter<T>,
    onItemClick: (item: T) -> Unit
): AdapterView.OnItemClickListener {
    return AdapterView.OnItemClickListener { _, _, position, _ ->
        onItemClick(adapter.getItem(position))
    }
}
