package org.kvxd.sondbord.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.kvxd.sondbord.service.DownloadProgress
import org.kvxd.sondbord.ui.theme.AppColors

@Composable
fun DownloaderDialog(
    downloadState: DownloadProgress,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp),
            backgroundColor = AppColors.Surface,
            contentColor = AppColors.Text,
            modifier = Modifier.width(480.dp).padding(16.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Download from YouTube", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = AppColors.TextDim)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Enter a URL to download audio (mp3). The file will be added to your library automatically.",
                    style = MaterialTheme.typography.caption,
                    color = AppColors.TextDim
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("https://youtube.com/watch?v=...", color = AppColors.TextDim) },
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = AppColors.Text,
                            cursorColor = AppColors.Accent,
                            focusedBorderColor = AppColors.Accent,
                            unfocusedBorderColor = AppColors.SurfaceLight,
                            backgroundColor = AppColors.Background
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if(url.isNotBlank()) onDownload(url) },
                        enabled = !downloadState.isActive,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AppColors.Accent,
                            disabledBackgroundColor = AppColors.SurfaceLight
                        ),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Download, null, tint = if(downloadState.isActive) AppColors.TextDim else Color.Black)
                    }
                }

                if (downloadState.isActive || downloadState.status != "Idle") {
                    Spacer(Modifier.height(16.dp))

                    Column(
                        Modifier.fillMaxWidth()
                            .background(AppColors.Background, RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = downloadState.status,
                                color = if(downloadState.status.contains("Error")) AppColors.Danger else AppColors.Accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (downloadState.isActive) {
                                Text("${(downloadState.progress * 100).toInt()}%", color = AppColors.Text, fontSize = 11.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = downloadState.progress,
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = AppColors.Accent,
                            backgroundColor = AppColors.SurfaceLight
                        )

                        if (downloadState.status.contains("Error") || downloadState.status.contains("Failed")) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = downloadState.log.takeLast(150),
                                color = AppColors.Danger,
                                fontSize = 10.sp,
                                maxLines = 3,
                                lineHeight = 14.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}