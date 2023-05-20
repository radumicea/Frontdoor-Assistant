package iiotca.frontdoorassistant.ui.main.blacklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.data.Result
import iiotca.frontdoorassistant.databinding.FragmentBlacklistBinding
import iiotca.frontdoorassistant.ui.main.MainViewModel
import iiotca.frontdoorassistant.ui.main.MainViewModelFactory
import iiotca.frontdoorassistant.ui.main.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlacklistFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentBlacklistBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentBlacklistBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        viewModel = ViewModelProvider(
            this, MainViewModelFactory(sharedViewModel)
        )[MainViewModel::class.java]
        viewModel.init()

        sharedViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.blacklist.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
                binding.blacklist.visibility = View.VISIBLE
            }
        }

        viewModel.getNamesResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val adapter = BlacklistAdapter(result.data)
                    binding.blacklist.adapter = adapter
                }

                is Result.Error -> {
                    Snackbar.make(binding.root, result.code, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.getBlacklistNames()
        }
    }
}