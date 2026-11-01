package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.DialogBrand

/**
 * Feedback inmediato al tocar un comando, **sin** [androidx.compose.ui.window.Dialog].
 * Así no tapa el diálogo nativo de SoftAP (WifiNetworkSpecifier).
 * Compacto, centrado y con alto contraste.
 */
@Composable
fun KilnCommandProgressBanner(
    phase: String,
    detail: String? = null,
    modifier: Modifier = Modifier
) {
    // Panel claro fijo + texto casi negro: contraste alto sobre cualquier fondo.
    val panelBg = Color(0xFFF8FAFC)
    val titleColor = Color(0xFF0F172A)
    val detailColor = Color(0xFF1E293B)
    val borderColor = Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = panelBg,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.5.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = DialogBrand.primaryButton(),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = phase,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Start
                    )
                    if (!detail.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = detailColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            ),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
