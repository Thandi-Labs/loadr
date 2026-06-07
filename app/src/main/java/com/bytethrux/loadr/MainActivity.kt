package com.bytethrux.loadr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun MainHomeView() {
    // 1. Keep track of the authentication state
    // In a real app, this might come from a ViewModel or DataStore
    var isLoggedIn by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // 2. Conditionally render the view based on state
        if (isLoggedIn) {
            HomePage(onLogout = { isLoggedIn = false })
        } else {
            // Your LoginView from the other file
            LoginView(onLoginSuccess = { isLoggedIn = true })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginViewPreview() {
    LoadrTheme {
        MainHomeView()
    }
}