package iiotca.frontdoorassistant.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.App
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.afterTextChanged
import iiotca.frontdoorassistant.data.DataSourceHelper
import iiotca.frontdoorassistant.data.Repository
import iiotca.frontdoorassistant.data.Result
import iiotca.frontdoorassistant.databinding.ActivityLoginBinding
import iiotca.frontdoorassistant.hideKeyboard
import iiotca.frontdoorassistant.ui.SharedViewModel
import iiotca.frontdoorassistant.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val preferences =
            App.getSharedPreferences(getString(R.string.preference_file_key))
        val tokenPreference =
            preferences.getString(getString(R.string.preference_token), null)
        val refreshTokenPreference =
            preferences.getString(getString(R.string.preference_refresh_token), null)

        if (tokenPreference != null && refreshTokenPreference != null) {
            Repository.token = tokenPreference
            Repository.refreshToken = refreshTokenPreference
            var ok: Result<Nothing?>
            runBlocking(Dispatchers.IO) {
                ok = DataSourceHelper.refreshToken(tokenPreference, refreshTokenPreference)
            }

            when (ok) {
                is Result.Success -> {
                    startActivity(Intent(this, MainActivity::class.java))

                    finish()
                    setResult(Activity.RESULT_OK)
                }
                is Result.Error -> {
                    with(
                        App.getSharedPreferences(App.getString(R.string.preference_file_key)).edit()
                    ) {
                        clear()
                        apply()
                    }
                    Snackbar.make(binding.root, R.string.login_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        val userName = binding.userName.editText!!
        val password = binding.password.editText!!
        val loginButton = binding.loginButton

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        loginViewModel =
            ViewModelProvider(
                this,
                LoginViewModelFactory(sharedViewModel)
            )[LoginViewModel::class.java]

        loginViewModel.loginFormState.observe(this@LoginActivity) { loginState ->
            // disable login button unless both userName / password is valid
            loginButton.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                userName.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        }

        loginViewModel.loginError.observe(this@LoginActivity) { loginError ->
            if (loginError != null) {
                binding.lockIcon.visibility = View.VISIBLE
                binding.loginTextView.visibility = View.VISIBLE
                binding.userName.visibility = View.VISIBLE
                binding.password.visibility = View.VISIBLE
                binding.loginButton.visibility = View.VISIBLE
                Snackbar.make(binding.root, loginError, Snackbar.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                setResult(Activity.RESULT_OK)
            }
        }

        sharedViewModel.isLoading.observe(this@LoginActivity) {
            if (it) {
                binding.lockIcon.visibility = View.GONE
                binding.loginTextView.visibility = View.GONE
                binding.userName.visibility = View.GONE
                binding.password.visibility = View.GONE
                binding.loginButton.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
            }
        }

        userName.afterTextChanged {
            loginViewModel.loginDataChanged(userName.text.toString(), password.text.toString())
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(userName.text.toString(), password.text.toString())
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        handleLogin(userName.text.toString(), password.text.toString())
                    }
                }
                false
            }

            loginButton.setOnClickListener {
                handleLogin(userName.text.toString(), password.text.toString())
            }
        }
    }

    private fun handleLogin(userName: String, password: String) {
        hideKeyboard()
        lifecycleScope.launch(Dispatchers.IO) { loginViewModel.login(userName, password) }
    }

    override fun onBackPressed() {
    }
}
