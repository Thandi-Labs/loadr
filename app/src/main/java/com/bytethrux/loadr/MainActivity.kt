package com.bytethrux.loadr

import android.Manifest
import android.os.Build
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
import com.bytethrux.loadr.data.local.SettingsDataStore
import com.bytethrux.loadr.data.local.SubscriptionStore
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.RetrofitClient
import com.bytethrux.loadr.data.repository.AuthRepository
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.OffersRepository
import com.bytethrux.loadr.data.repository.SubscriptionsRepository
import com.bytethrux.loadr.data.sim.SimManager
import com.bytethrux.loadr.data.transactions.StatusFilter
import com.bytethrux.loadr.data.ussd.AirtimeBalanceProvider
import com.bytethrux.loadr.data.ussd.UssdExecutor
import com.bytethrux.loadr.ui.auth.AuthViewModel
import com.bytethrux.loadr.ui.common.ComingSoonScreen
import com.bytethrux.loadr.ui.home.HomeScreen
import com.bytethrux.loadr.ui.home.HomeViewModel
import com.bytethrux.loadr.ui.offers.OffersScreen
import com.bytethrux.loadr.ui.offers.OffersViewModel
import com.bytethrux.loadr.ui.settings.SettingsScreen
import com.bytethrux.loadr.ui.settings.SettingsViewModel
import com.bytethrux.loadr.ui.subscriptions.SubscriptionsScreen
import com.bytethrux.loadr.ui.subscriptions.SubscriptionsViewModel
import com.bytethrux.loadr.ui.theme.LoadrTheme
import com.bytethrux.loadr.ui.theme.ThemeMode
import com.bytethrux.loadr.ui.transactions.TransactionsScreen
import com.bytethrux.loadr.ui.transactions.TransactionsViewModel
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val tokenDataStore by lazy { TokenDataStore(applicationContext) }
    private val settingsDataStore by lazy { SettingsDataStore(applicationContext) }
    private val authRepository by lazy { AuthRepository(RetrofitClient.instance, tokenDataStore) }
    private val homeRepository by lazy { HomeRepository(RetrofitClient.instance, tokenDataStore) }
    private val offersRepository by lazy { OffersRepository(RetrofitClient.instance, tokenDataStore) }
    private val airtimeBalanceProvider by lazy { AirtimeBalanceProvider(applicationContext) }
    private val subscriptionStore by lazy { SubscriptionStore(applicationContext) }
    private val subscriptionsRepository by lazy {
        SubscriptionsRepository(RetrofitClient.instance, tokenDataStore, subscriptionStore)
    }

    private val authViewModel by viewModels<AuthViewModel> {
        AuthViewModel.Factory(authRepository)
    }
    private val homeViewModel by viewModels<HomeViewModel> {
        HomeViewModel.Factory(
            homeRepository,
            airtimeBalanceProvider,
            subscriptionStore,
            subscriptionsRepository,
            settingsDataStore,
        )
    }
    private val offersViewModel by viewModels<OffersViewModel> {
        OffersViewModel.Factory(offersRepository)
    }
    private val settingsViewModel by viewModels<SettingsViewModel> {
        SettingsViewModel.Factory(settingsDataStore, SimManager.activeSims(applicationContext))
    }
    private val subscriptionsViewModel by viewModels<SubscriptionsViewModel> {
        SubscriptionsViewModel.Factory(
            subscriptionsRepository, settingsDataStore, UssdExecutor(applicationContext)
        )
    }
    private val transactionsViewModel by viewModels<TransactionsViewModel> {
        TransactionsViewModel.Factory(homeRepository)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled silently; service degrades gracefully if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RetrofitClient.initialize(tokenDataStore)

        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            LoadrTheme(themeMode = ThemeMode.fromString(settings.themeMode)) {
                Surface {
                    MainHomeView(
                        authViewModel,
                        homeViewModel,
                        offersViewModel,
                        settingsViewModel,
                        subscriptionsViewModel,
                        transactionsViewModel,
                    )
                }
            }
        }
    }
}

@Composable
fun MainHomeView(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    offersViewModel: OffersViewModel,
    settingsViewModel: SettingsViewModel,
    subscriptionsViewModel: SubscriptionsViewModel,
    transactionsViewModel: TransactionsViewModel,
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var showSplash by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("Home") }
    var transactionsFilter by remember { mutableStateOf(StatusFilter.ALL) }

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
                        onNavigate = { screen -> currentScreen = screen },
                        onOpenTransactions = { filter ->
                            transactionsFilter = filter
                            currentScreen = "Transactions"
                        },
                        onOpenSubscriptions = { currentScreen = "Subscriptions" }
                    )
                }
                "Offers" -> {
                    LaunchedEffect(Unit) { offersViewModel.refresh() }
                    OffersScreen(
                        viewModel = offersViewModel,
                        onBackClick = { currentScreen = "Home" }
                    )
                }
                "Settings" -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        username = uiState.username ?: "Agent",
                        onBackClick = { currentScreen = "Home" }
                    )
                }
                "Subscriptions" -> {
                    SubscriptionsScreen(
                        viewModel = subscriptionsViewModel,
                        onBackClick = { currentScreen = "Home" }
                    )
                }
                "Transactions" -> {
                    LaunchedEffect(Unit) {
                        transactionsViewModel.setInitialFilters(transactionsFilter)
                        transactionsViewModel.refresh()
                    }
                    TransactionsScreen(
                        viewModel = transactionsViewModel,
                        onBackClick = { currentScreen = "Home" }
                    )
                }
                else -> {
                    // Features on the roadmap but not built yet
                    ComingSoonScreen(
                        title = currentScreen,
                        onBackClick = { currentScreen = "Home" }
                    )
                }
            }
        }
        else -> LoginView(viewModel = authViewModel, onLoginSuccess = {})
    }
}
