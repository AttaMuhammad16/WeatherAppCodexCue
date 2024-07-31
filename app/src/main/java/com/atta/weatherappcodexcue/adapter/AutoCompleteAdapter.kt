package com.atta.weatherappcodexcue.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class AutoCompleteAdapter(context: Context, private val placesClient: PlacesClient) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {

    private val autocompleteFilter = AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES).build()

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val predictions = getAutocompletePredictions(constraint.toString())
                        val predictionStrings = predictions.map { it.getFullText(null).toString() }
                        filterResults.values = predictionStrings
                        filterResults.count = predictionStrings.size
                    }
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                if (results!= null && results.count > 0) {
                    @Suppress("UNCHECKED_CAST")
                    addAll(results.values as List<String>)
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }



    suspend fun getAutocompletePredictions(query: String): List<AutocompletePrediction> {
        val token = AutocompleteSessionToken.newInstance()
        val request = com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest.builder().setQuery(query).setSessionToken(token).build()

        return suspendCancellableCoroutine { continuation ->
            val autocompletePredictions = placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                    continuation.resume(response.autocompletePredictions)
                }.addOnFailureListener { exception ->
                    if (exception is ApiException) {
                        // Handle API error
                    }
                    continuation.resumeWithException(exception)
                }
        }
    }
}