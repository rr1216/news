package com.app.util

import android.widget.Toast
import com.app.NewsApplication

fun String.showToast() {
    try {
        Toast.makeText(NewsApplication.context, this, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}