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
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Repository.token != "" && Repository.refreshToken != "") {
            var res: Result<Nothing?>
            runBlocking(Dispatchers.IO) {
                res = DataSourceHelper.refreshToken()
            }

            when (res) {
                is Result.Success -> {
                    startActivity(Intent(context, MainActivity::class.java))

                    requireActivity().finish()
                    requireActivity().setResult(Activity.RESULT_OK)
                }

                is Result.Error -> {
                    Repository.token = ""
                    Repository.refreshToken = ""
                    Snackbar.make(
                        binding.root,
                        (res as Result.Error<Nothing?>).code,
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }

        val userName = binding.userName.editText!!
        val password = binding.password.editText!!
        val loginButton = binding.loginButton
        val changePassword = binding.changePassword

        viewModel = ViewModelProvider(this)[AuthenticateViewModel::class.java]
        viewModel.init()

        viewModel.loginError.observe(viewLifecycleOwner) { errorId ->
            if (errorId != null) {
                Snackbar.make(binding.root, errorId, Snackbar.LENGTH_SHORT).show()
            } else {
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
                binding.changePassword.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
                binding.lockIcon.visibility = View.VISIBLE
                binding.loginTextView.visibility = View.VISIBLE
                binding.userName.visibility = View.VISIBLE
                binding.password.visibility = View.VISIBLE
                binding.loginButton.visibility = View.VISIBLE
                binding.changePassword.visibility = View.VISIBLE
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

            changePassword.setOnClickListener {
                findNavController().navigate(R.id.action_navigate_to_change_password)
            }
        }
    }

    private fun handleLogin(userName: String, password: String) {
        hideKeyboard()
        lifecycleScope.launch(Dispatchers.IO) { viewModel.login(userName, password) }
    }
}
