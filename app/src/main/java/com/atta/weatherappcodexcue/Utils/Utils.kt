package com.atta.weatherappcodexcue.Utils

import android.app.Activity
import android.content.Context
import androidx.core.content.ContextCompat
import com.atta.weatherappcodexcue.R

object Utils {
    fun Activity.setStatusBarColor(color:Int = R.color.black){
        window.statusBarColor=ContextCompat.getColor(this,color)
    }
}