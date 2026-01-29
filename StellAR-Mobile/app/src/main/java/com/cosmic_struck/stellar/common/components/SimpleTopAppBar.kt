package com.cosmic_struck.stellar.common.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.util.Rajdhani

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopAppBar(
    title: String,
    popNavigation: ()-> Unit,
    modifier: Modifier = Modifier) {

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        title = {
            Text(
                text = title,
                fontFamily = Rajdhani,
                fontSize = 24.sp,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {popNavigation()}
            ) {
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}

@Preview
@Composable
fun SimpleTopAppBarPreview(){
    SimpleTopAppBar(
        title = "Title",
        popNavigation = {}
    )
}