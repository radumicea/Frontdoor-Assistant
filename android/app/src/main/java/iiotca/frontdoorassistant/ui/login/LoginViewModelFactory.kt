package iiotca.frontdoorassistant.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import iiotca.frontdoorassistant.ui.SharedViewModel

class LoginViewModelFactory(private val sharedViewModel: SharedViewModel) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(sharedViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}