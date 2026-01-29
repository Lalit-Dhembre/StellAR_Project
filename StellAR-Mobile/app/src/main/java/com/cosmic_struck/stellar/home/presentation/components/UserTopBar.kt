package com.cosmic_struck.stellar.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

// Educational Theme Colors
private val EduPrimary = Color(0xFF5C6BC0)
private val EduTextPrimary = Color(0xFF1A1A2E)
private val EduTextSecondary = Color(0xFF6B7280)

@Composable
fun UserTopBar(
    userName: String = "Student",
    userLevel: String = "1",
    userPic: String = "",
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Hello, $userName 👋",
                    fontFamily = Rajdhani,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduTextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Level badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EduPrimary, Color(0xFF7E57C2))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⭐ Level $userLevel",
                            fontFamily = Rajdhani,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Explorer",
                        fontFamily = Rajdhani,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduTextSecondary
                    )
                }
            }

            // Right: Profile Picture - Clickable
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
                    .background(
                        Brush.linearGradient(
                            colors = listOf(EduPrimary, Color(0xFF7E57C2))
                        )
                    )
                    .padding(3.dp)
            ) {
                CoilImage(
                    imageModel = { userPic.ifEmpty { "https://ui-avatars.com/api/?name=$userName&background=5C6BC0&color=fff" } },
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    ),
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@Preview
@Composable
fun UserTopBarPreview() {
    UserTopBar(
        userName = "John",
        userLevel = "5"
    )
}