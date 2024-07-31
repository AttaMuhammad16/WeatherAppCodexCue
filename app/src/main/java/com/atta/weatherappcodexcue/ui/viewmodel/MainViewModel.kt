package com.atta.weatherappcodexcue.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel:ViewModel() {
    private var _setPlaceName:MutableStateFlow<String> = MutableStateFlow("")
    val getPlaceName:StateFlow<String> = _setPlaceName

    fun setPlaceName(name:String){
        _setPlaceName.value=name
    }
}