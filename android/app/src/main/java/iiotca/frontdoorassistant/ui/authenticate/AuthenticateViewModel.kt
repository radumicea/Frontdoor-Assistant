package iiotca.frontdoorassistant.ui.authenticate

import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import iiotca.frontdoorassistant.App.Companion.getString
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.AuthenticateDataSource
import iiotca.frontdoorassistant.data.Result

class AuthenticateViewModel : ViewModel() {
    private lateinit var _loginError: MutableLiveData<Int?>
    val loginError: LiveData<Int?>
        get() = _loginError

    private lateinit var _changePasswordError: MutableLiveData<Int?>
    val changePasswordError: LiveData<Int?>
        get() = _changePasswordError

    private lateinit var _isLoading: MutableLiveData<Boolean>
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun init() {
        _loginError = MutableLiveData()
        _changePasswordError = MutableLiveData()
        _isLoading = MutableLiveData()
    }

    fun login(userName: String, password: String) {
        _isLoading.postValue(true)
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
        _isLoading.postValue(false)
    }

    fun changePassword(userName: String, oldPassword: String, newPassword: String) {
        _isLoading.postValue(true)
        when (val res = AuthenticateDataSource.changePassword(userName, oldPassword, newPassword)) {
            is Result.Success -> {
                _changePasswordError.postValue(null)
            }
            is Result.Error -> {
                if (res.code == 401) {
                    _changePasswordError.postValue(R.string.wrong_credentials)
                } else {
                    _changePasswordError.postValue(R.string.change_password_failed)
                }
            }
        }
        _isLoading.postValue(false)
    }

    fun loginDataChanged(userName: EditText, password: EditText, button: Button) {
        button.isEnabled = true

        if (!isUserNameValid(userName.text.toString())) {
            userName.error = getString(R.string.invalid_userName)
            button.isEnabled = false
        }
        if (!isPasswordValid(password.text.toString())) {
            password.error = getString(R.string.invalid_password)
            button.isEnabled = false
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

    fun changePasswordDataChanged(
        userName: EditText,
        oldPassword: EditText,
        newPassword: EditText,
        confirmPassword: EditText,
        button: Button
    ) {
        button.isEnabled = true

        if (!isUserNameValid(userName.text.toString())) {
            userName.error = getString(R.string.invalid_userName)
            button.isEnabled = false
        }
        if (!isPasswordValid(oldPassword.text.toString())) {
            oldPassword.error = getString(R.string.invalid_password)
            button.isEnabled = false
        }
        if (!isPasswordValid(newPassword.text.toString())) {
            newPassword.error = getString(R.string.invalid_password)
            button.isEnabled = false
        }
        if (!isPasswordValid(confirmPassword.text.toString())) {
            confirmPassword.error = getString(R.string.invalid_password)
            button.isEnabled = false
        }
        if (confirmPassword.text.toString() != newPassword.text.toString()) {
            confirmPassword.error = getString(R.string.passwords_dont_match)
            button.isEnabled = false
        }
    }
}