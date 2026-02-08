package org.kvxd.bonk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import org.kvxd.bonk.ui.theme.AppColors

@Composable
fun VolumeControl(
    label: String,
    value: Float,
    max: Float,
    color: Color = AppColors.Accent,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = AppColors.TextDim, fontSize = 12.sp)
            Text("${value.toInt()}%", color = AppColors.Text, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..max,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
    }
}