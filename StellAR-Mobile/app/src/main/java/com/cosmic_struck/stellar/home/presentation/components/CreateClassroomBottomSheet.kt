package com.cosmic_struck.stellar.home.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.ui.theme.ButtonPressed
import com.cosmic_struck.stellar.ui.theme.ButtonPrimary

// Educational Theme Colors
private val EduPrimary = Color(0xFF5C6BC0)
private val EduBackground = Color(0xFFF8F9FE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClassroomBottomSheet(
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modalSheetState: Boolean,
    onValueChange: (String) -> Unit,
    classroomNameText: String,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (modalSheetState) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            containerColor = EduBackground
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 20.dp)
                ) {
                    // Title
                    Text(
                        text = "Create a Classroom",
                        color = Color(0xFF1A1A2E),
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle
                    Text(
                        text = "Enter a name for your new classroom",
                        color = Color(0xFF6B7280),
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Text Field
                    OutlinedTextField(
                        value = classroomNameText,
                        onValueChange = { onValueChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        placeholder = {
                            Text(
                                text = "e.g., Physics 101, Biology Class",
                                color = Color.Gray,
                                fontFamily = Rajdhani,
                                fontWeight = FontWeight.Normal
                            )
                        },
                        label = {
                            Text(
                                text = "Classroom Name",
                                fontFamily = Rajdhani,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EduPrimary,
                            focusedLabelColor = EduPrimary,
                            cursorColor = EduPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Create Button
                    Button(
                        onClick = {
                            if (!isLoading && classroomNameText.isNotBlank()) {
                                onSubmit()
                                onDismiss()
                            }
                        },
                        enabled = !isLoading && classroomNameText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = EduPrimary.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.height(24.dp)
                            )
                        } else {
                            Text(
                                text = "Create Classroom",
                                fontFamily = Rajdhani,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Info text
                    Text(
                        text = "A unique join code will be generated for your classroom",
                        color = Color(0xFF9CA3AF),
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
