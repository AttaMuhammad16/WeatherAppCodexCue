package com.atta.weatherappcodexcue.Utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.atta.weatherappcodexcue.R
import com.atta.weatherappcodexcue.api.ApiInterface
import com.atta.weatherappcodexcue.models.WeatherApp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object Utils {
    var BASEURL="https://api.openweathermap.org/data/2.5/"

    fun Activity.setStatusBarColor(color:Int = R.color.black){
        window.statusBarColor=ContextCompat.getColor(this,color)
    }

    suspend fun fetchWeather(cityName:String,context: Context):WeatherApp {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASEURL)
            .build().create(ApiInterface::class.java)
        val weatherAppModel = retrofit.getWeatherData(cityName, context.getString(R.string.api_key), "metric").await()
        return weatherAppModel
    }

    fun TextView.animateTextChange(newText: String) {
        val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 500
            fillAfter = true
        }
        val fadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 500
            fillAfter = true
        }
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                this@animateTextChange.text = newText
                this@animateTextChange.startAnimation(fadeIn)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        this@animateTextChange.startAnimation(fadeOut)
    }


    fun View.animateFromBottomToTop(duration: Long = 1000) {
        val height = this.height.toFloat()

        val animation = TranslateAnimation(
            0f, // fromXDelta
            0f, // toXDelta
            height, // fromYDelta
            0f // toYDelta
        ).apply {
            this.duration = duration
            this.fillAfter = true
        }

        animation.interpolator=AccelerateInterpolator()
        this.startAnimation(animation)
    }

    fun View.fadeInFadeOut() {
        val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 500
            fillAfter = true
        }
        val fadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 500
            fillAfter = true
        }
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                this@fadeInFadeOut.startAnimation(fadeIn)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        this@fadeInFadeOut.startAnimation(fadeOut)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTimeForOffset(offsetSeconds: Int):String {
        val zoneOffset = ZoneOffset.ofTotalSeconds(offsetSeconds)
        val currentTime = ZonedDateTime.now(zoneOffset)
        val formatter = DateTimeFormatter.ofPattern("hh:mm a\nMM/dd/yyyy")
        return currentTime.format(formatter)
    }

    fun convertDate(dateInMilliseconds: String, dateFormat: String?): String {
        return DateFormat.format(dateFormat, dateInMilliseconds.toLong()).toString()
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }


    @SuppressLint("SimpleDateFormat")
    fun formatSunTimes(sunrise: Long, sunset: Long, timeZoneOffset: Int): Pair<String, String> {
        // Convert Unix timestamps to Date objects
        val sunriseDate = Date(sunrise * 1000)
        val sunsetDate = Date(sunset * 1000)

        // Create a SimpleDateFormat instance
        val sdf = SimpleDateFormat("hh:mm a")

        // Calculate the time zone offset in hours and create a TimeZone object
        val offsetHours = timeZoneOffset / 3600
        val timeZone = TimeZone.getTimeZone("GMT${if (offsetHours >= 0) "+" else "-"}${Math.abs(offsetHours)}")
        sdf.timeZone = timeZone

        // Format the dates
        val sunriseTimeLocal = sdf.format(sunriseDate)
        val sunsetTimeLocal = sdf.format(sunsetDate)

        return Pair(sunriseTimeLocal, sunsetTimeLocal)
    }


}