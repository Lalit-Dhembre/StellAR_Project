package com.cosmic_struck.stellar.auth.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.AuthScreenCaptions
import com.cosmic_struck.stellar.common.util.Rajdhani

@Composable
fun LowerPortion(
    navigateToLoginAccount: () -> Unit,
    navigateToCreateAccount: () -> Unit,
    modifier: Modifier = Modifier) {

    Box(modifier = modifier
        .fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "Welcome To Classroom",
                fontSize = 28.sp,
                fontFamily = Rajdhani,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = AuthScreenCaptions,
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )

            Button(
                onClick = {
                    navigateToCreateAccount()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = "Create an Account",
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Text(
                    text = "Already Have an Account?",
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White
                )

                TextButton(
                    onClick = {
                        navigateToLoginAccount()
                    }
                ) {
                    Text(
                        text = "Log in",
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Rajdhani,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}