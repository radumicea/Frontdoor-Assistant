package iiotca.frontdoorassistant.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun setLoading() {
        _isLoading.postValue(true)
    }

    fun resetLoading() {
        _isLoading.postValue(false)
    }
}