package iiotca.frontdoorassistant.ui.main.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.MainDataSource
import iiotca.frontdoorassistant.data.Result
import iiotca.frontdoorassistant.data.dto.Location
import iiotca.frontdoorassistant.databinding.FragmentSettingsBinding
import iiotca.frontdoorassistant.ui.main.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            sharedViewModel.isLoading.postValue(true)
            askLocationPermissionAndHandleSetLocation()
        }
    }

    private fun askLocationPermissionAndHandleSetLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchAndSetLocation()
        } else {
            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            fetchAndSetLocation()
        } else {
            sharedViewModel.isLoading.postValue(false)
            Toast.makeText(
                context, R.string.please_allow_location, Toast.LENGTH_LONG
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchAndSetLocation() {
        val locProvider = LocationServices.getFusedLocationProviderClient(requireActivity())
        locProvider.getCurrentLocation(
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            lifecycleScope.launch(Dispatchers.IO) {
                when (val res = MainDataSource.setLocation(
                    Location(location.latitude.toFloat(), location.longitude.toFloat())
                )) {
                    is Result.Success -> {
                        sharedViewModel.isLoading.postValue(false)
                        Snackbar.make(binding.root, R.string.success, Toast.LENGTH_SHORT).show()
                    }

                    is Result.Error -> {
                        sharedViewModel.isLoading.postValue(false)
                        Snackbar.make(
                            binding.root, res.code, Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                sharedViewModel.isLoading.postValue(false)
            }
        }.addOnFailureListener {
            sharedViewModel.isLoading.postValue(false)
            Snackbar.make(binding.root, R.string.error_fetch_location, Toast.LENGTH_SHORT).show()
        }
    }
}