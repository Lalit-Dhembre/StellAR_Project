package com.cosmic_struck.stellar.auth.presentation.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.auth.presentation.viewmodel.AuthViewModel
import com.cosmic_struck.stellar.common.util.Rajdhani

// Educational Theme Colors
private val EduPrimary = Color(0xFF5C6BC0)
private val EduBackground = Color(0xFFF8F9FE)
private val EduSurface = Color(0xFFFFFFFF)
private val EduTextPrimary = Color(0xFF1A1A2E)
private val EduTextSecondary = Color(0xFF6B7280)
private val EduSuccess = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreenEmailValidation(
    navigateback: () -> Unit = {},
    setImageUri: (Uri?) -> Unit,
    navigateToPasswordValidation: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$"
    val isEmailValid = email.matches(emailPattern.toRegex())
    val isUsernameValid = username.length >= 3
    val isFormValid = isEmailValid && isUsernameValid

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        setImageUri(uri)
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
                // Header
                Text(
                    text = "Create Account 🎓",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Rajdhani,
                    color = EduTextPrimary
                )

                Text(
                    text = "Step 1: Your Profile",
                    fontSize = 14.sp,
                    color = EduTextSecondary,
                    fontFamily = Rajdhani,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Image Picker
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EduPrimary.copy(alpha = 0.2f),
                                    Color(0xFF7E57C2).copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(3.dp, EduPrimary, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📷", fontSize = 32.sp)
                            Text(
                                text = "Add Photo",
                                fontSize = 12.sp,
                                color = EduPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Username Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Username",
                        color = EduTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = Rajdhani,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Choose a username",
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
                            Text(text = "👤", fontSize = 18.sp)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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

                Spacer(modifier = Modifier.height(32.dp))

                // Continue Button
                Button(
                    onClick = {
                        viewModel.setEmailAddress(email)
                        viewModel.setUsername(username)
                        navigateToPasswordValidation()
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
                        text = "Continue",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreenPasswordValidation(
    navigateback: () -> Unit = {},
    navigateToHomeScreen: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    val hasMinLength = password.length >= 8
    val hasNumber = password.contains(Regex("[0-9]"))
    val hasSymbol = password.contains(Regex("[!@#$%^&*(),.?\":{}|<>]"))

    val checksPassed = listOf(hasMinLength, hasNumber, hasSymbol).count { it }
    val progressTarget = when (checksPassed) {
        1 -> 0.33f
        2 -> 0.66f
        3 -> 1f
        else -> 0f
    }

    val animatedProgress by animateFloatAsState(targetValue = progressTarget, label = "progress")
    val progressColor by animateColorAsState(
        targetValue = when (checksPassed) {
            3 -> EduSuccess
            2 -> Color(0xFFFFA726)
            1 -> Color(0xFFEF5350)
            else -> Color.Gray
        }, label = "color"
    )

    val state = viewModel.state.collectAsState().value

    LaunchedEffect(state.success) {
        if (state.success) {
            Log.d("CreateAccount", "Account created successfully")
            navigateToHomeScreen()
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
                    .padding(horizontal = 24.dp)
            ) {
                // Header
                Text(
                    text = "Secure Password 🔐",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Rajdhani,
                    color = EduTextPrimary
                )

                Text(
                    text = "Step 2: Create a strong password",
                    fontSize = 14.sp,
                    color = EduTextSecondary,
                    fontFamily = Rajdhani,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

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
                                text = "Create a password",
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

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = Color(0xFFE0E0E0)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Validation Checklist
                ValidationRow(
                    label = "At least 8 characters",
                    isValid = hasMinLength
                )
                Spacer(modifier = Modifier.height(12.dp))
                ValidationRow(
                    label = "Contains a number",
                    isValid = hasNumber
                )
                Spacer(modifier = Modifier.height(12.dp))
                ValidationRow(
                    label = "Contains a symbol (!@#$%)",
                    isValid = hasSymbol
                )

                Spacer(modifier = Modifier.weight(1f))

                // Create Account Button
                Button(
                    onClick = {
                        viewModel.setPassword(password)
                        viewModel.signUpWithEmail()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = checksPassed == 3,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduSuccess,
                        disabledContainerColor = EduSuccess.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Create Account",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "🚀", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ValidationRow(label: String, isValid: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isValid) EduSuccess.copy(alpha = 0.1f) 
                else Color(0xFFE0E0E0).copy(alpha = 0.5f)
            )
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isValid) EduSuccess else Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isValid) "✓" else "",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            fontFamily = Rajdhani,
            fontSize = 14.sp,
            color = if (isValid) EduSuccess else EduTextSecondary,
            fontWeight = if (isValid) FontWeight.Medium else FontWeight.Normal
        )
    }
}