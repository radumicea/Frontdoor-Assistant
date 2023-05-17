package iiotca.frontdoorassistant.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.AuthenticateDataSource
import iiotca.frontdoorassistant.data.Result
import iiotca.frontdoorassistant.ui.SharedViewModel

class LoginViewModel(private val sharedViewModel: SharedViewModel) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginError = MutableLiveData<Int?>()
    val loginError: LiveData<Int?> = _loginError

    fun login(userName: String, password: String) {
        sharedViewModel.setLoading()
        when (val res = AuthenticateDataSource.login(userName, password)) {
            is Result.Success -> {
                _loginError.postValue(null)
            }
            is Result.Error -> {
                if (res.code == 401) {
                    _loginError.postValue(R.string.wrong_credentials)
                } else {
                    _loginError.postValue(R.string.login_failed)
                }
            }
        }
        sharedViewModel.resetLoading()
    }

    fun loginDataChanged(userName: String, password: String) {
        if (!isUserNameValid(userName)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_userName)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    private fun isUserNameValid(userName: String): Boolean {
        val r = Regex("""^[a-zA-Z]{2,50}$""")
        return r matches userName
    }

    private fun isPasswordValid(password: String): Boolean {
        val r =
            Regex("""^(?=.*[0-9])(?=.*[a-z])(?=.*['"\\`*!@$%^#&(){}\[\]:;<>,.?/~_+\-=|]).{6,}$""")
        return r matches password
    }
}