package com.cosmic_struck.stellar.home.presentation.components

import android.content.Context
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.ui.theme.ButtonPressed
import com.cosmic_struck.stellar.ui.theme.ButtonPrimary
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.text.Text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinClassroomBottomSheet(
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modalSheetState : Boolean,
    onValueChange: (String) -> Unit,
    codeText: String,
    modifier: Modifier = Modifier) {

    if(modalSheetState) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                ) {
                    Text(
                        text = "Join A Classroom",
                        color = Color.Black,
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(
                        modifier = Modifier
                            .height(10.dp)
                    )

                    TextField(
                        value = codeText,
                        onValueChange = { it ->
                            onValueChange(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .clip(RoundedCornerShape(25.dp)),
                        placeholder = {
                            Text(
                                text = "Enter Join Code",
                                color = Color.Gray,
                                fontFamily = Rajdhani,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onSubmit()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPressed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Join",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

}