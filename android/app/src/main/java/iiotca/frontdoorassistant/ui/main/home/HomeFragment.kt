package iiotca.frontdoorassistant.ui.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentHomeBinding

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

        binding.blacklist.setOnClickListener {
            Snackbar.make(view, "Hello", Snackbar.LENGTH_SHORT).show()
        }
        binding.history.setOnClickListener {
            Snackbar.make(view, "Hello", Snackbar.LENGTH_SHORT).show()
        }
        binding.settings.setOnClickListener {
            Snackbar.make(view, "Hello", Snackbar.LENGTH_SHORT).show()
        }
    }
}