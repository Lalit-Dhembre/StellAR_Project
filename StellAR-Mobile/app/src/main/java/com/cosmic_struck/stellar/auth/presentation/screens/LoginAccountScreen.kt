package com.cosmic_struck.stellar.auth.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.auth.presentation.viewmodel.AuthViewModel
import com.cosmic_struck.stellar.common.util.Rajdhani

// Educational Theme Colors
private val EduPrimary = Color(0xFF5C6BC0)
private val EduBackground = Color(0xFFF8F9FE)
private val EduSurface = Color(0xFFFFFFFF)
private val EduTextPrimary = Color(0xFF1A1A2E)
private val EduTextSecondary = Color(0xFF6B7280)
private val EduError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginAccountScreen(
    navigateback: () -> Unit = {},
    navigateToHomeScreen: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    
    val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$"
    val isEmailValid = email.matches(emailPattern.toRegex())
    val isPasswordValid = password.length >= 8
    val isFormValid = isEmailValid && isPasswordValid
    
    val state = viewModel.state.collectAsState().value

    LaunchedEffect(state.success) {
        if (state.success) {
            navigateToHomeScreen()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error.isNotEmpty()) {
            showError = true
        }
    }

    Scaffold(
        containerColor = EduBackground,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = navigateback) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.back),
                                contentDescription = "Back",
                                tint = EduTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FE),
                            Color(0xFFE8EAF6)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Header
                Text(
                    text = "Welcome Back! 👋",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Rajdhani,
                    color = EduTextPrimary
                )

                Text(
                    text = "Sign in to continue learning",
                    fontSize = 16.sp,
                    color = EduTextSecondary,
                    fontFamily = Rajdhani,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Email Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Email Address",
                        color = EduTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = Rajdhani,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Enter your email",
                                color = EduTextSecondary,
                                fontFamily = Rajdhani
                            )
                        },
                        textStyle = TextStyle(
                            color = EduTextPrimary,
                            fontFamily = Rajdhani,
                            fontSize = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EduPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = EduSurface,
                            unfocusedContainerColor = EduSurface,
                            cursorColor = EduPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        leadingIcon = {
                            Text(text = "📧", fontSize = 18.sp)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Password Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Password",
                        color = EduTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = Rajdhani,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Enter your password",
                                color = EduTextSecondary,
                                fontFamily = Rajdhani
                            )
                        },
                        textStyle = TextStyle(
                            color = EduTextPrimary,
                            fontFamily = Rajdhani,
                            fontSize = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EduPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = EduSurface,
                            unfocusedContainerColor = EduSurface,
                            cursorColor = EduPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Text(text = "🔒", fontSize = 18.sp)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isVisible = !isVisible }) {
                                Text(
                                    text = if (isVisible) "👁️" else "👁️‍🗨️",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    )
                }

                // Error message
                if (showError && state.error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EduError.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "❌ ${state.error}",
                            color = EduError,
                            fontSize = 14.sp,
                            fontFamily = Rajdhani
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Login Button
                Button(
                    onClick = {
                        viewModel.setEmailAddress(email)
                        viewModel.setPassword(password)
                        viewModel.signInWithEmail()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduPrimary,
                        disabledContainerColor = EduPrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Sign In",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Forgot password
                Text(
                    text = "Forgot your password?",
                    color = EduPrimary,
                    fontSize = 14.sp,
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}