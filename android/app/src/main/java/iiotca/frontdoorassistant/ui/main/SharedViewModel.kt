package iiotca.frontdoorassistant.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.AuthenticateDataSource
import iiotca.frontdoorassistant.data.Result

class SharedViewModel : ViewModel() {
    val isLoading = MutableLiveData<Boolean>()

    private val _logOutError = MutableLiveData<Int?>()
    val logOutError: LiveData<Int?> = _logOutError

    fun logOut() {
        isLoading.postValue(true)
        when (AuthenticateDataSource.logOut()) {
            is Result.Success -> {
                _logOutError.postValue(null)
            }

            else -> {
                _logOutError.postValue(R.string.request_failed)
            }
        }
        isLoading.postValue(false)
    }
}