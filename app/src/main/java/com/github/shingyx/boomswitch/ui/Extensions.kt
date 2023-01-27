package com.github.shingyx.boomswitch.ui

import android.widget.AdapterView

fun adapterOnItemClick(
    onItemClick: (position: Int) -> Unit
): AdapterView.OnItemClickListener {
    return AdapterView.OnItemClickListener { _, _, position, _ ->
        onItemClick(position)
    }
}
