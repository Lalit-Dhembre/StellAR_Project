package com.cosmic_struck.stellar.classroom.presentation.components
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.util.Rajdhani


@Composable
fun MenuScreen(
    onSummaryButtonClick: () -> Unit,
    onNotesButtonClick: () -> Unit,
    onQuizButtonClick: () -> Unit
) {
    // Main Container with Dark Blue Background
    Column(
        modifier = Modifier
            .background(Color.Transparent)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top Row: Summary and Notes
        Box(){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuButton(
                    modifier = Modifier
                        .weight(1f),
                    text = "SUMMARY",
                    iconRes = R.drawable.notebook, // Replace with your icon
                    onClick = { onSummaryButtonClick() }
                )

                MenuButton(
                    modifier = Modifier
                        .weight(1f),
                    text = "NOTES",
                    iconRes = R.drawable.pdf, // Replace with your icon
                    onClick = { onNotesButtonClick() }
                )
            }
        }


        Spacer(modifier = Modifier.height(48.dp))

        // Bottom Row: Quiz
        MenuButton(
            modifier = Modifier,
            text = "QUIZ",
            iconRes = R.drawable.quiz, // Replace with your icon
            onClick = { onQuizButtonClick() }
        )
    }
}

@Composable
fun MenuButton(
    modifier: Modifier,
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent, // Transparent inside
        shape = RoundedCornerShape(20.dp), // Fully rounded capsule shape
        border = BorderStroke(2.dp, Color.White), // White outline
        modifier = modifier
            .clickable { onClick() }
        // Optional: Set a min width to make them uniform if needed
        // .widthIn(min = 160.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Icon
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = text,
                modifier = Modifier.size(24.dp), // Adjust size based on your assets
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Text label
            Text(
                text = text,
                fontFamily = Rajdhani,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

