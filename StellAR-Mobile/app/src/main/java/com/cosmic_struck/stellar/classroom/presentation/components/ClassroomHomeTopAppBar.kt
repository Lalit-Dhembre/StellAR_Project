package com.cosmic_struck.stellar.classroom.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani

// Educational Theme Colors
private val EduPrimary = Color(0xFF5C6BC0)
private val EduTextPrimary = Color(0xFF1A1A2E)
private val EduTextSecondary = Color(0xFF6B7280)

@Composable
fun ClassroomTopAppBar(
    classroomName: String = "Classroom",
    classroomAuthor: String = "Creator",
    classroomMembers: String = "0",
    classroomCode: String? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Classroom name with icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EduPrimary, Color(0xFF7E57C2))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏫", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = classroomName,
                        fontFamily = Rajdhani,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduTextPrimary
                    )
                    Text(
                        text = "by $classroomAuthor",
                        fontFamily = Rajdhani,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduTextSecondary
                    )
                }
            }

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Members count
                StatChip(
                    emoji = "👥",
                    label = classroomMembers,
                    sublabel = "members"
                )

                // Code chip (if available)
                if (classroomCode != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(EduPrimary.copy(alpha = 0.1f))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(classroomCode))
                                Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📋", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = classroomCode,
                                fontFamily = Rajdhani,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    emoji: String,
    label: String,
    sublabel: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8EAF6))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label $sublabel",
                fontFamily = Rajdhani,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = EduTextSecondary
            )
        }
    }
}
