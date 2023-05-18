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
import iiotca.frontdoorassistant.ui.SharedViewModel

class AuthenticateViewModel(private val sharedViewModel: SharedViewModel) : ViewModel() {
    private val _loginError = MutableLiveData<Int?>()
    val loginError: LiveData<Int?> = _loginError

    private val _changePasswordError = MutableLiveData<Int?>()
    val changePasswordError: LiveData<Int?> = _changePasswordError

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

    fun changePassword(userName: String, oldPassword: String, newPassword: String) {
        sharedViewModel.setLoading()
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
        sharedViewModel.resetLoading()
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