package app.platecarbon

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import app.platecarbon.databinding.ActivityMainBinding
import androidx.navigation.ui.NavigationUI

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Window Insets için padding ayarı
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Navigation Controller'ı ayarla
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Navbar'ı Navigation ile bağla
        // Bu kısım onCreate içinde bir defa çağrılır (sadece 1 kere!)
        binding.bottomNavigation.setupWithNavController(navController)

// Geri tuşuna özel dinleyici
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recent_vehicles -> {
                    navController.navigate(R.id.recentVehiclesFragment)
                    true
                }

                else -> {
                    // Diğerleri zaten nav_graph ile eşleşiyor
                    NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
        }


        // Bazı fragment'larda navbar'ı gizle
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Örnek: Ayarlar veya diğer gizli ekranlar
                // R.id.settingsFragment -> binding.bottomNavigation.visibility = View.GONE
                else -> binding.bottomNavigation.visibility = View.VISIBLE
            }
        }
    }
}