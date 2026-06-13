package com.bytethrux.loadr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.bytethrux.loadr.ui.theme.LoadrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoadrTheme {
                Surface {
                    MainHomeView()
                }
            }
        }
    }
}

@Composable
fun MainHomeView() {
    var showSplash by remember { mutableStateOf(value = true) }
    var isLoggedIn by remember { mutableStateOf(value = false) }

    if (showSplash) {
        SplashScreen { showSplash = false }
    } else if (isLoggedIn) {
        HomePage(onLogout = { isLoggedIn = false })
    } else {
        LoginView(onLoginSuccess = { isLoggedIn = true })
    }
}

@Preview(showBackground = true)
@Composable
fun LoginViewPreview() {
    LoadrTheme {
        MainHomeView()
    }
}