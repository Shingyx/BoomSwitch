package com.github.shingyx.boomswitch.ui

import android.widget.BaseAdapter

abstract class TypedAdapter<T> : BaseAdapter() {
    abstract override fun getItem(position: Int): T
}
