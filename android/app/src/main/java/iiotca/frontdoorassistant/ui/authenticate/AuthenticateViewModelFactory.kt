package iiotca.frontdoorassistant.ui.authenticate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import iiotca.frontdoorassistant.ui.SharedViewModel

class AuthenticateViewModelFactory(private val sharedViewModel: SharedViewModel) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthenticateViewModel::class.java)) {
            return AuthenticateViewModel(sharedViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}