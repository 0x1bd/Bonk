package org.kvxd.bonk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kvxd.bonk.model.Microphone
import org.kvxd.bonk.ui.theme.AppColors

@Composable
fun MicSelector(
    microphones: List<Microphone>,
    selectedId: String?,
    onSelect: (Microphone) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedMic = microphones.find { it.id == selectedId }
    val displayText = selectedMic?.description ?: "System Default"

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = AppColors.SurfaceLight),
            contentPadding = PaddingValues(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(
                text = displayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AppColors.Text,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, null, tint = AppColors.Text, modifier = Modifier.size(20.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.SurfaceLight).width(230.dp)
        ) {
            microphones.forEach { mic ->
                DropdownMenuItem(onClick = {
                    onSelect(mic)
                    expanded = false
                }) {
                    Text(mic.description, color = AppColors.Text, fontSize = 12.sp)
                }
            }
        }
    }
}