package com.atta.weatherappcodexcue.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.atta.weatherappcodexcue.R
import com.atta.weatherappcodexcue.Utils.Utils.fetchWeather
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
import java.util.Locale
class MainActivity : AppCompatActivity() {
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: AutoCompleteAdapter
    private lateinit var predictionsList: List<AutocompletePrediction>
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var progressDialog: ProgressDialog

    val calendar = Calendar.getInstance()
    @SuppressLint("SimpleDateFormat")
    val dayFormat = SimpleDateFormat("EEEE")
    val dayOfWeek: String = dayFormat.format(calendar.time)
    @SuppressLint("SimpleDateFormat")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy")
    val currentDate: String = dateFormat.format(calendar.time)


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

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        lifecycleScope.launch {
            mainViewModel.getPlaceName.collect { placeName ->
                if (placeName.isNotEmpty()) {
                    try {
                        val weather = fetchWeather(placeName, this@MainActivity)
                        binding.locationNameTv.text = placeName
                        progressDialog.dismiss()
                        val temperature = weather.main.temp.toString()
                        binding.temperatureTv.text = "$temperature ℃"

                        val weatherType = weather.weather[0]
                        when (weatherType.main) {
                            "Rain" -> binding.lottieAnimationView.setAnimation(R.raw.rainy)
                            "Clear" -> binding.lottieAnimationView.setAnimation(R.raw.sunny)
                            "Clouds" -> binding.lottieAnimationView.setAnimation(R.raw.cloudy)
                        }
                        binding.lottieAnimationView.repeatCount = LottieDrawable.INFINITE
                        binding.lottieAnimationView.playAnimation()
                        binding.weatherTypeTv.text = weatherType.description
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