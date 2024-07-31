package com.atta.weatherappcodexcue.ui.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import com.atta.weatherappcodexcue.R
import com.atta.weatherappcodexcue.Utils.Utils.fetchWeather
import com.atta.weatherappcodexcue.Utils.Utils.setStatusBarColor
import com.atta.weatherappcodexcue.adapter.AutoCompleteAdapter
import com.atta.weatherappcodexcue.ui.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: AutoCompleteAdapter
    lateinit var predictionsList: List<AutocompletePrediction>
    val mainViewModel:MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setStatusBarColor()

        val etSearch = findViewById<AutoCompleteTextView>(R.id.etSearch)
        val crossIcon = findViewById<ImageView>(R.id.crossIcon)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(this, getString(R.string.api))
        placesClient = Places.createClient(this)

        adapter = AutoCompleteAdapter(this, placesClient)
        etSearch.setAdapter(adapter)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (etSearch.text.isEmpty()){
                    crossIcon.visibility = View.GONE
                }else{
                    crossIcon.visibility = View.VISIBLE
                }
            }
            override fun afterTextChanged(et: Editable?) {
                lifecycleScope.launch {
                    val query = et?.toString() ?: ""
                    predictionsList = adapter.getAutocompletePredictions(query)
                    adapter.clear()
                    adapter.addAll(predictionsList.map {it.getFullText(null).toString()})
                }
            }
        })

        etSearch.setOnItemClickListener { _, _, position, _ ->
            val placeId = predictionsList[position].placeId
            val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val placeName = response.place.name
                lifecycleScope.launch {
                    if (placeName!=null){
                        mainViewModel.setPlaceName(placeName)
                    }else{
                        Toast.makeText(this@MainActivity, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        crossIcon.setOnClickListener {
            etSearch.setText("")
        }


        lifecycleScope.launch {
            mainViewModel.getPlaceName.collect{
                if (it.isNotEmpty()){
                    val weather=fetchWeather(it,this@MainActivity)
                    Log.i("TAG", "onCreate:$weather")
                }
            }
        }


    }
}