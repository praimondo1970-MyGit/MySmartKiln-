package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors

/**
 * Pantalla de búsqueda en curso (misma estética que el escaneo BLE histórico).
 */
@Composable
fun KilnSearchInProgressOverlay(
    title: String,
    subtitleLine1: String,
    subtitleLine2: String,
    subtitleLine3: String,
    modifier: Modifier = Modifier,
    bottomNav: @Composable () -> Unit = {},
    homeIndicator: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.Primary100)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_sin_hornos),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-165).dp)
                .size(200.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color(0xFF1E3A8A),
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 400.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 325.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subtitleLine1,
                style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF1E3A8A)),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitleLine2,
                style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF1E3A8A)),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitleLine3,
                style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF1E3A8A)),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary700,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }

        bottomNav()
        homeIndicator()
    }
}
