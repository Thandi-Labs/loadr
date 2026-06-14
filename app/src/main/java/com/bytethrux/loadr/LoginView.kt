package com.bytethrux.loadr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytethrux.loadr.ui.auth.AuthViewModel
import com.bytethrux.loadr.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoginView(onLoginSuccess: () -> Unit, viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoadrNavy)
    ) {
        // Hero section with dot grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spacing = 28.dp.toPx()
                val dotRadius = 1.5.dp.toPx()
                var x = spacing / 2
                while (x < size.width) {
                    var y = spacing / 2
                    while (y < size.height) {
                        drawCircle(
                            color = LoadrGreen.copy(alpha = 0.07f),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += spacing
                    }
                    x += spacing
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LoadrGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val cx = size.width / 2
                        drawLine(LoadrNavy, Offset(2.dp.toPx(), size.height), Offset(cx, 3.dp.toPx()), 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(LoadrNavy, Offset(cx, 3.dp.toPx()), Offset(size.width - 2.dp.toPx(), size.height), 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(LoadrNavy, Offset(6.dp.toPx(), 14.dp.toPx()), Offset(size.width - 6.dp.toPx(), 14.dp.toPx()), 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        append("Load")
                        withStyle(SpanStyle(color = LoadrGreen)) { append("r") }
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoadrWhite
                )
            }
        }

        // Bottom sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(LoadrNavyCard)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Welcome back", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LoadrWhite)
                Text("Sign in to your agent account", fontSize = 13.sp, color = LoadrSlate)
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A1018))
                        .padding(12.dp)
                ) {
                    Text(error, color = LoadrRed, fontSize = 13.sp)
                }
                LaunchedEffect(error) {
                    delay(3000)
                    viewModel.clearError()
                }
            }

            // Username field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Username", fontSize = 11.sp, color = LoadrSlate, letterSpacing = 0.5.sp)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("your_username", color = LoadrNavySurface, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoadrGreen,
                        unfocusedBorderColor = LoadrNavySurface,
                        focusedTextColor = LoadrWhite,
                        unfocusedTextColor = LoadrMint,
                        cursorColor = LoadrGreen,
                        focusedContainerColor = LoadrNavy,
                        unfocusedContainerColor = LoadrNavy,
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Password field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Password", fontSize = 11.sp, color = LoadrSlate, letterSpacing = 0.5.sp)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    placeholder = { Text("••••••••", color = LoadrNavySurface, fontSize = 14.sp) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Outlined.VisibilityOff
                                else
                                    Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = LoadrSlate
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoadrGreen,
                        unfocusedBorderColor = LoadrNavySurface,
                        focusedTextColor = LoadrWhite,
                        unfocusedTextColor = LoadrMint,
                        cursorColor = LoadrGreen,
                        focusedContainerColor = LoadrNavy,
                        unfocusedContainerColor = LoadrNavy,
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            TextButton(
                onClick = { /* TODO: forgot password */ },
                modifier = Modifier.align(Alignment.End).padding(top = 0.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Forgot password?", fontSize = 12.sp, color = LoadrGreen)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        viewModel.login(username, password)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LoadrGreen),
                enabled = username.isNotEmpty() && password.isNotEmpty() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LoadrNavy, strokeWidth = 2.dp)
                } else {
                    Text("Sign in", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LoadrNavy)
                }
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LoadrSlate)) { append("New agent? ") }
                    withStyle(SpanStyle(color = LoadrGreen)) { append("Contact your distributor") }
                },
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
