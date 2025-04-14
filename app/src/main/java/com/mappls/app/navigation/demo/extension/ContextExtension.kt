package com.mappls.app.navigation.demo.extension


import android.content.Context
import android.view.WindowManager

val Context.windowManager: WindowManager
    get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager