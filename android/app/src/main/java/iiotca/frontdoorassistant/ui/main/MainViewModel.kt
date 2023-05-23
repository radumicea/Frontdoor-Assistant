package iiotca.frontdoorassistant.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.MainDataSource
import iiotca.frontdoorassistant.data.Result
import iiotca.frontdoorassistant.data.dto.BlacklistEntry
import iiotca.frontdoorassistant.data.dto.HistoryItem

class MainViewModel(private val sharedViewModel: SharedViewModel) : ViewModel() {
    private lateinit var _getNamesResult: MutableLiveData<Result<MutableList<String>>>
    val getNamesResult get() = _getNamesResult

    private lateinit var _removeError: MutableLiveData<Int?>
    val removeError get() = _removeError

    private lateinit var _addError: MutableLiveData<Int?>
    val addError get() = _addError

    private lateinit var _getHistoryResult: MutableLiveData<Result<List<HistoryItem>>>
    val getHistoryResult get() = _getHistoryResult

    fun init() {
        _getNamesResult = MutableLiveData()
        _removeError = MutableLiveData()
        _addError = MutableLiveData()
        _getHistoryResult = MutableLiveData()
    }

    fun getBlacklistNames() {
        sharedViewModel.isLoading.postValue(true)
        when (val res = MainDataSource.getBlacklistNames()) {
            is Result.Success -> {
                res.data.sort()
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

    fun removeFromBlacklist(items: List<String>) {
        sharedViewModel.isLoading.postValue(true)
        when (val res = MainDataSource.removeFromBlacklist(items)) {
            is Result.Success -> {
                _removeError.postValue(null)
            }

            is Result.Error -> {
                if (res.code == 401) {
                    _removeError.postValue(R.string.session_expired_cannot_refresh)
                } else {
                    _removeError.postValue(R.string.request_failed)
                }
            }
        }
        sharedViewModel.isLoading.postValue(false)
    }

    fun addToBlacklist(paths: List<String>, name: String) {
        sharedViewModel.isLoading.postValue(true)
        when (val res = MainDataSource.addToBlacklist(BlacklistEntry(paths, name))) {
            is Result.Success -> {
                _addError.postValue(null)
            }

            is Result.Error -> {
                if (res.code == 401) {
                    _addError.postValue(R.string.session_expired_cannot_refresh)
                } else {
                    _addError.postValue(R.string.request_failed)
                }
            }
        }
        sharedViewModel.isLoading.postValue(false)
    }

    fun getHistory() {
        sharedViewModel.isLoading.postValue(true)
        when (val res = MainDataSource.getHistory()) {
            is Result.Success -> {
                _getHistoryResult.postValue(res)
            }

            is Result.Error -> {
                if (res.code == 401) {
                    _getHistoryResult.postValue(Result.Error(R.string.session_expired_cannot_refresh))
                } else {
                    _getHistoryResult.postValue(Result.Error(R.string.request_failed))
                }
            }
        }
        sharedViewModel.isLoading.postValue(false)
    }
}