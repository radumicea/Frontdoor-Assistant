package iiotca.frontdoorassistant.ui.authenticate.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.App
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.afterTextChanged
import iiotca.frontdoorassistant.data.DataSourceHelper
import iiotca.frontdoorassistant.data.Repository
import iiotca.frontdoorassistant.data.Result
import iiotca.frontdoorassistant.databinding.FragmentLoginBinding
import iiotca.frontdoorassistant.hideKeyboard
import iiotca.frontdoorassistant.ui.authenticate.AuthenticateViewModel
import iiotca.frontdoorassistant.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LoginFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var viewModel: AuthenticateViewModel
    private lateinit var binding: FragmentLoginBinding
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        this.inflater = inflater
        navController = findNavController()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    startActivity(Intent(context, MainActivity::class.java))

                    requireActivity().finish()
                    requireActivity().setResult(Activity.RESULT_OK)
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
        val forgotPassword = binding.forgotPassword

        viewModel = ViewModelProvider(this)[AuthenticateViewModel::class.java]

        viewModel.loginError.observe(viewLifecycleOwner) { loginError ->
            if (loginError != null) {
                binding.lockIcon.visibility = View.VISIBLE
                binding.loginTextView.visibility = View.VISIBLE
                binding.userName.visibility = View.VISIBLE
                binding.password.visibility = View.VISIBLE
                binding.loginButton.visibility = View.VISIBLE
                binding.forgotPassword.visibility = View.VISIBLE
                Snackbar.make(binding.root, loginError, Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, R.string.success, Snackbar.LENGTH_SHORT).show()
                startActivity(Intent(context, MainActivity::class.java))
                requireActivity().finish()
                requireActivity().setResult(Activity.RESULT_OK)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.lockIcon.visibility = View.GONE
                binding.loginTextView.visibility = View.GONE
                binding.userName.visibility = View.GONE
                binding.password.visibility = View.GONE
                binding.loginButton.visibility = View.GONE
                binding.forgotPassword.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
            }
        }

        userName.afterTextChanged {
            viewModel.loginDataChanged(userName, password, loginButton)
        }

        password.apply {
            afterTextChanged {
                viewModel.loginDataChanged(userName, password, loginButton)
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

            forgotPassword.setOnClickListener {
                navController.navigate(R.id.action_navigate_to_change_password)
            }
        }
    }

    private fun handleLogin(userName: String, password: String) {
        hideKeyboard()
        lifecycleScope.launch(Dispatchers.IO) { viewModel.login(userName, password) }
    }
}
