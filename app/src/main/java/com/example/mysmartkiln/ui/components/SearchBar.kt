package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar",
    onSearchClick: () -> Unit = {}
) {
    val kilnColors = LocalKilnColors.current
    val isEsp = kilnColors.isEsp
    val shape = RoundedCornerShape(if (isEsp) 10.dp else 12.dp)
    val barHeight = if (isEsp) 40.dp else 48.dp
    val horizontalPadding = if (isEsp) 12.dp else 14.dp
    val textSize = if (isEsp) 14.sp else 15.sp
    val iconSize = if (isEsp) 18.dp else 20.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isEsp) {
                    Modifier
                } else {
                    Modifier.shadow(elevation = 2.dp, shape = shape)
                }
            )
            .background(color = kilnColors.cardBackground, shape = shape)
            .border(width = 1.dp, color = kilnColors.cardBorder, shape = shape)
            .height(barHeight)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Search",
                modifier = Modifier
                    .size(iconSize)
                    .clickable { onSearchClick() },
                tint = kilnColors.muted
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = kilnColors.title,
                        fontSize = textSize
                    ),
                    cursorBrush = SolidColor(kilnColors.accent),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    color = kilnColors.muted,
                                    fontSize = textSize
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchBarPreview() {
    MySmartKilnTheme {
        SearchBar(
            query = "",
            onQueryChange = {},
            placeholder = "Buscar programas...",
            modifier = Modifier.padding(16.dp)
        )
    }
}
