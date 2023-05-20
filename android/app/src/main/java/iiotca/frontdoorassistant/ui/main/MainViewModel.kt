package iiotca.frontdoorassistant.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.MainDataSource
import iiotca.frontdoorassistant.data.Result

class MainViewModel(private val sharedViewModel: SharedViewModel) : ViewModel() {
    private lateinit var _getNamesResult: MutableLiveData<Result<MutableList<String>>>
    val getNamesResult get() = _getNamesResult

    fun init() {
        _getNamesResult = MutableLiveData()
    }

    fun getBlacklistNames() {
        sharedViewModel.isLoading.postValue(true)
        when (val res = MainDataSource.getBlacklistNames()) {
            is Result.Success -> {
                _getNamesResult.postValue(res)
            }

            is Result.Error -> {
                if (res.code == 401) {
                    _getNamesResult.postValue(Result.Error(R.string.session_expired_cannot_refresh))
                } else {
                    _getNamesResult.postValue(Result.Error(R.string.request_failed))
                }
            }
        }
        sharedViewModel.isLoading.postValue(false)
    }
}