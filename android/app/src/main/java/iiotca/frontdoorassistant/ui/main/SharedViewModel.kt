package iiotca.frontdoorassistant.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.AuthenticateDataSource
import iiotca.frontdoorassistant.data.Result

class SharedViewModel : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _logOutError = MutableLiveData<Int?>()
    val logOutError: LiveData<Int?> = _logOutError

    fun logOut() {
        _isLoading.postValue(true)
        when (AuthenticateDataSource.logOut()) {
            is Result.Success -> {
                _logOutError.postValue(null)
            }

            else -> {
                _logOutError.postValue(R.string.log_out_failed)
            }
        }
        _isLoading.postValue(false)
    }
}