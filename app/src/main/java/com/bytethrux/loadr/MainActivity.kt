package com.bytethrux.loadr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.RetrofitClient
import com.bytethrux.loadr.data.repository.AuthRepository
import com.bytethrux.loadr.ui.auth.AuthViewModel
import com.bytethrux.loadr.ui.theme.LoadrTheme
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val tokenDataStore by lazy { TokenDataStore(applicationContext) }
    private val authRepository by lazy { AuthRepository(RetrofitClient.instance, tokenDataStore) }
    private val authViewModel by viewModels<AuthViewModel> {
        AuthViewModel.Factory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoadrTheme {
                Surface {
                    MainHomeView(authViewModel)
                }
            }
        }
    }
}

@Composable
fun MainHomeView(authViewModel: AuthViewModel) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var showSplash by remember { mutableStateOf(true) }

    when {
        showSplash -> SplashScreen(onFinished = {showSplash = false })
        uiState.isLoggedIn -> HomePage(onLogout = { authViewModel.logout() })
        else -> LoginView(viewModel = authViewModel, onLoginSuccess = {})
    }
}

