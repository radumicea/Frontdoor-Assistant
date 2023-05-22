package iiotca.frontdoorassistant.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.databinding.ActivityMainBinding
import iiotca.frontdoorassistant.ui.authenticate.AuthenticateActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.main_toolbar))

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupActionBarWithNavController(navController)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_log_out) {
                    lifecycleScope.launch(Dispatchers.IO) { sharedViewModel.logOut() }
                    return true
                }

                return false
            }
        })

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        sharedViewModel.isLoading.observe(this) {
            if (it) {
                binding.mainToolbar.menu.setGroupEnabled(0, false)
            } else {
                binding.mainToolbar.menu.setGroupEnabled(0, true)
            }
        }

        sharedViewModel.logOutError.observe(this) { errorId ->
            if (errorId != null) {
                Snackbar.make(binding.root, errorId, Snackbar.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, AuthenticateActivity::class.java))
                finish()
                setResult(Activity.RESULT_OK)
            }
        }

        onBackPressedDispatcher.addCallback(
            this@MainActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (sharedViewModel.isLoading.value == true) return
                    navController.navigateUp()
                }
            })

        askNotificationPermission()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (sharedViewModel.isLoading.value == true) {
            return false
        }
        return navController.navigateUp()
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this, R.string.please_allow_notifications, Toast.LENGTH_LONG
            ).show()
        }
    }
}