package iiotca.frontdoorassistant.ui.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.databinding.FragmentHomeBinding
import iiotca.frontdoorassistant.ui.main.SharedViewModel

class HomeFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentHomeBinding
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        sharedViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.title.visibility = View.GONE
                binding.blacklist.visibility = View.GONE
                binding.history.visibility = View.GONE
                binding.settings.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
                binding.title.visibility = View.VISIBLE
                binding.blacklist.visibility = View.VISIBLE
                binding.history.visibility = View.VISIBLE
                binding.settings.visibility = View.VISIBLE
            }
        }

        binding.blacklist.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_blacklist)
        }
        binding.history.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_history)
        }
        binding.settings.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_settings)
        }
    }
}