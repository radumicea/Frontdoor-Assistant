package iiotca.frontdoorassistant.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.databinding.FragmentSettingsBinding
import iiotca.frontdoorassistant.ui.main.MainViewModel
import iiotca.frontdoorassistant.ui.main.SharedViewModel

class SettingsFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.init()

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        sharedViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.setLocation.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
                binding.setLocation.visibility = View.VISIBLE
            }
        }

        binding.setLocation.setOnClickListener {
            Snackbar.make(view, "Hello", Snackbar.LENGTH_SHORT).show()
        }
    }
}