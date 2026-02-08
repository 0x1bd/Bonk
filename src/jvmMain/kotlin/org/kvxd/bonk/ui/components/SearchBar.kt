package org.kvxd.bonk.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.kvxd.bonk.ui.theme.AppColors

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(Icons.Default.Search, null, tint = AppColors.TextDim) },
        placeholder = { Text(placeholder, color = AppColors.TextDim) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = AppColors.Text,
            focusedBorderColor = AppColors.Accent,
            unfocusedBorderColor = AppColors.SurfaceLight,
            backgroundColor = AppColors.Surface
        )
    )
}