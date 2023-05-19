package iiotca.frontdoorassistant.ui.authenticate.changepassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.afterTextChanged
import iiotca.frontdoorassistant.databinding.FragmentChangePasswordBinding
import iiotca.frontdoorassistant.hideKeyboard
import iiotca.frontdoorassistant.ui.authenticate.AuthenticateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChangePasswordFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentChangePasswordBinding
    private lateinit var viewModel: AuthenticateViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[AuthenticateViewModel::class.java]

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.lockIcon.visibility = View.GONE
                binding.changePasswordTextView.visibility = View.GONE
                binding.userName.visibility = View.GONE
                binding.oldPassword.visibility = View.GONE
                binding.newPassword.visibility = View.GONE
                binding.confirmPassword.visibility = View.GONE
                binding.changePasswordButton.visibility = View.GONE
                binding.cancelButton.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
            }
        }

        viewModel.changePasswordError.observe(viewLifecycleOwner) { errorId ->
            if (errorId != null) {
                binding.lockIcon.visibility = View.VISIBLE
                binding.changePasswordTextView.visibility = View.VISIBLE
                binding.userName.visibility = View.VISIBLE
                binding.oldPassword.visibility = View.VISIBLE
                binding.newPassword.visibility = View.VISIBLE
                binding.confirmPassword.visibility = View.VISIBLE
                binding.changePasswordButton.visibility = View.VISIBLE
                binding.cancelButton.visibility = View.VISIBLE
                Snackbar.make(binding.root, errorId, Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, R.string.success, Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        val userName = binding.userName.editText!!
        val oldPassword = binding.oldPassword.editText!!
        val newPassword = binding.newPassword.editText!!
        val confirmPassword = binding.confirmPassword.editText!!

        userName.afterTextChanged {
            viewModel.changePasswordDataChanged(
                userName,
                oldPassword,
                newPassword,
                confirmPassword,
                binding.changePasswordButton
            )
        }

        oldPassword.afterTextChanged {
            viewModel.changePasswordDataChanged(
                userName,
                oldPassword,
                newPassword,
                confirmPassword,
                binding.changePasswordButton
            )

        }

        newPassword.afterTextChanged {
            viewModel.changePasswordDataChanged(
                userName,
                oldPassword,
                newPassword,
                confirmPassword,
                binding.changePasswordButton
            )

        }

        confirmPassword.apply {
            afterTextChanged {
                viewModel.changePasswordDataChanged(
                    userName,
                    oldPassword,
                    newPassword,
                    confirmPassword,
                    binding.changePasswordButton
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        handleChangePassword(
                            userName.text.toString(),
                            oldPassword.text.toString(),
                            newPassword.text.toString()
                        )
                    }
                }
                false
            }

            binding.changePasswordButton.setOnClickListener {
                handleChangePassword(
                    userName.text.toString(),
                    oldPassword.text.toString(),
                    newPassword.text.toString()
                )
            }

            binding.cancelButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun handleChangePassword(userName: String, oldPassword: String, newPassword: String) {
        hideKeyboard()
        lifecycleScope.launch(Dispatchers.IO) { viewModel.changePassword(userName, oldPassword, newPassword) }
    }
}