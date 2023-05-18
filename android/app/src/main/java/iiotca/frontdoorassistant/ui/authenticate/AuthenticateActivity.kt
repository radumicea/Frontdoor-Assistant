package iiotca.frontdoorassistant.ui.authenticate

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.databinding.ActivityAuthenticateBinding
import iiotca.frontdoorassistant.ui.SharedViewModel

class AuthenticateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthenticateBinding
    private lateinit var navController: NavController
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthenticateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        onBackPressedDispatcher.addCallback(
            this@AuthenticateActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (sharedViewModel.isLoading.value == true) return
                    navController.navigateUp()
                }
            })
    }
}
