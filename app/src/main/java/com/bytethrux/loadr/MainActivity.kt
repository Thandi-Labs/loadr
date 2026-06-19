package com.bytethrux.loadr

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.RetrofitClient
import com.bytethrux.loadr.data.repository.AuthRepository
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.OffersRepository
import com.bytethrux.loadr.ui.auth.AuthViewModel
import com.bytethrux.loadr.ui.home.HomeScreen
import com.bytethrux.loadr.ui.home.HomeViewModel
import com.bytethrux.loadr.ui.offers.OffersScreen
import com.bytethrux.loadr.ui.offers.OffersViewModel
import com.bytethrux.loadr.ui.theme.LoadrTheme
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val tokenDataStore by lazy { TokenDataStore(applicationContext) }
    private val authRepository by lazy { AuthRepository(RetrofitClient.instance, tokenDataStore) }
    private val homeRepository by lazy { HomeRepository(RetrofitClient.instance, tokenDataStore) }
    private val offersRepository by lazy { OffersRepository(RetrofitClient.instance, tokenDataStore) }

    private val authViewModel by viewModels<AuthViewModel> {
        AuthViewModel.Factory(authRepository)
    }
    private val homeViewModel by viewModels<HomeViewModel> {
        HomeViewModel.Factory(homeRepository)
    }
    private val offersViewModel by viewModels<OffersViewModel> {
        OffersViewModel.Factory(offersRepository)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled silently; service degrades gracefully if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RetrofitClient.initialize(tokenDataStore)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
            )
        )

        setContent {
            LoadrTheme {
                Surface {
                    MainHomeView(authViewModel, homeViewModel, offersViewModel)
                }
            }
        }
    }
}

@Composable
fun MainHomeView(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    offersViewModel: OffersViewModel
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var showSplash by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("Home") }

    when {
        showSplash -> SplashScreen(onFinished = { showSplash = false })
        uiState.isLoggedIn -> {
            when (currentScreen) {
                "Home" -> {
                    LaunchedEffect(Unit) { homeViewModel.refresh() }
                    HomeScreen(
                        viewModel = homeViewModel,
                        username = uiState.username ?: "Agent",
                        onLogout = { authViewModel.logout() },
                        onNavigate = { screen -> currentScreen = screen }
                    )
                }
                "Offers" -> {
                    LaunchedEffect(Unit) { offersViewModel.refresh() }
                    OffersScreen(
                        viewModel = offersViewModel,
                        onBackClick = { currentScreen = "Home" }
                    )
                }
                else -> {
                    // Fallback or other screens
                    HomeScreen(
                        viewModel = homeViewModel,
                        username = uiState.username ?: "Agent",
                        onLogout = { authViewModel.logout() },
                        onNavigate = { screen -> currentScreen = screen }
                    )
                }
            }
        }
        else -> LoginView(viewModel = authViewModel, onLoginSuccess = {})
    }
}

