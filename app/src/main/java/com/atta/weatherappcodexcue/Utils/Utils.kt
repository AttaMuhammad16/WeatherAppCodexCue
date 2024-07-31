package com.atta.weatherappcodexcue.Utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
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
import java.util.Calendar
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

}