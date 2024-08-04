package com.atta.weatherappcodexcue.ui.activities

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.atta.weatherappcodexcue.R
import com.atta.weatherappcodexcue.Utils.Utils.animateFromBottomToTop
import com.atta.weatherappcodexcue.Utils.Utils.animateTextChange
import com.atta.weatherappcodexcue.Utils.Utils.convertDate
import com.atta.weatherappcodexcue.Utils.Utils.fadeInFadeOut
import com.atta.weatherappcodexcue.Utils.Utils.fetchWeather
import com.atta.weatherappcodexcue.Utils.Utils.formatSunTimes
import com.atta.weatherappcodexcue.Utils.Utils.getCurrentTimeForOffset
import com.atta.weatherappcodexcue.Utils.Utils.hideKeyboard
import com.atta.weatherappcodexcue.Utils.Utils.setStatusBarColor
import com.atta.weatherappcodexcue.adapter.AutoCompleteAdapter
import com.atta.weatherappcodexcue.databinding.ActivityMainBinding
import com.atta.weatherappcodexcue.ui.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity() {

    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: AutoCompleteAdapter
    private lateinit var predictionsList: List<AutocompletePrediction>
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var progressDialog: ProgressDialog

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                checkLocationSettings()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                checkLocationSettings()
            }
            else -> {
                Toast.makeText(this, "No location access granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBarColor(R.color.splash_color)

        progressDialog = ProgressDialog(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Places API
        Places.initialize(this, getString(R.string.api))
        placesClient = Places.createClient(this)

        adapter = AutoCompleteAdapter(this, placesClient)
        binding.etSearch.setAdapter(adapter)



        // Add text changed listener for search field

        binding.etSearch.addTextChangedListener { editable ->
            if (editable.isNullOrEmpty()) {
                binding.crossIcon.visibility = View.GONE
            } else {
                binding.crossIcon.visibility = View.VISIBLE
                fetchAutocompletePredictions(editable.toString())
            }
        }

        binding.etSearch.setOnItemClickListener { _, _, position, _ ->
            val placeId = predictionsList[position].placeId
            val placeFields = listOf(Place.Field.NAME)
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            binding.etSearch.hideKeyboard()
            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val placeName = response.place.name
                val previousName=mainViewModel.getPlaceName.value
                if (previousName!=placeName){
                    if (placeName != null) {
                        progressDialog.setTitle("Loading")
                        progressDialog.setMessage("Please wait while we fetch your location.")
                        progressDialog.setCancelable(false)
                        progressDialog.show()
                        mainViewModel.setPlaceName(placeName)
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.crossIcon.setOnClickListener {
            binding.etSearch.setText("")
        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        observeViewModel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    private fun observeViewModel() {
        lifecycleScope.launch {
            mainViewModel.getPlaceName.collect { placeName ->
                if (placeName.isNotEmpty()) {
                    try {

                        val weather = fetchWeather(placeName, this@MainActivity)

                        binding.locationNameTv.animateTextChange(placeName)
                        progressDialog.dismiss()

                        val temperature = weather.main.temp.toString()
                        val newText="$temperature ℃"
                        binding.temperatureTv.animateTextChange(newText)

                        val weatherType = weather.weather[0]
                        var anim=-19273
                        anim = when (weatherType.main) {
                            "Rain" -> R.raw.rainy
                            "Clear" -> R.raw.sunny
                            "Clouds" -> R.raw.cloudy
                            "Smoke" -> R.raw.cloudy
                            else->{
                                R.raw.cloudy
                            }
                        }

                        if (anim!=-19273){
                            binding.lottieAnimationView.setAnimation(anim)
                            binding.lottieAnimationView.repeatCount = LottieDrawable.INFINITE
                            binding.lottieAnimationView.playAnimation()
                        }

                        binding.weatherTypeTv.animateTextChange(weatherType.description)
                        binding.highTemperature.animateTextChange("High Temperature: ${weather.main.temp_max} ℃")
                        binding.lowTemperature.animateTextChange("Low Temperature: ${weather.main.temp_min} ℃")

                        val currentTime=getCurrentTimeForOffset(weather.timezone)
                        binding.date.animateTextChange(currentTime)

                        binding.humidityValue.animateTextChange(weather.main.humidity.toString())
                        binding.windValue.animateTextChange( weather.wind.speed.toString())
                        binding.weatherCondition.animateTextChange(weatherType.main)

                        binding.weatherConditionAnim.setAnimation(anim)
                        binding.weatherConditionAnim.repeatCount = LottieDrawable.INFINITE
                        binding.weatherConditionAnim.playAnimation()
                        binding.currentTimeIn.animateTextChange("Current time in ${weather.name}")

                        val  sunRise= weather.sys.sunrise
                        val  sunSet= weather.sys.sunset
                        val (sunriseTime, sunsetTime) = formatSunTimes(sunRise.toLong(), sunSet.toLong(),weather.timezone)

                        binding.sunrise.animateTextChange(sunriseTime)
                        binding.sunset.animateTextChange(sunsetTime)

                        binding.sea.animateTextChange(weather.main.sea_level.toString())
                        binding.pressure.animateTextChange(weather.main.pressure.toString())

                    }catch (e:Exception){
                        Toast.makeText(this@MainActivity, e.message.toString(), Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                } else {
                    progressDialog.dismiss()
                }
            }
        }
    }

    private fun fetchAutocompletePredictions(query: String) {
        lifecycleScope.launch {
            predictionsList = adapter.getAutocompletePredictions(query)
            adapter.clear()
            adapter.addAll(predictionsList.map { it.getFullText(null).toString() })
        }
    }


    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 10000L).build()
        val locationSettingsRequest = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest).addOnSuccessListener {
               getCurrentLocation()
           }.addOnFailureListener {
               val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
               startActivity(intent)
               finish()
           }
    }


    private fun getCurrentLocation() {
        try {
            val locationResult: Task<Location> = fusedLocationClient.lastLocation
            locationResult.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val location: Location = task.result
                    val latitude = location.latitude
                    val longitude = location.longitude
                    getAddressFromLocation(latitude, longitude)
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val subAdminArea = address.subAdminArea
            if (!subAdminArea.isNullOrEmpty()) {
                progressDialog.setTitle("Loading")
                progressDialog.setMessage("Please wait while we fetch your location.")
                progressDialog.setCancelable(false)
                progressDialog.show()
                mainViewModel.setPlaceName(subAdminArea)
            } else {
                Toast.makeText(this, "Unable to get location name", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
        }
    }

}