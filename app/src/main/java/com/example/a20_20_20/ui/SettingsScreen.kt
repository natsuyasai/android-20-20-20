package com.example.a20_20_20.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.a20_20_20.domain.TimerSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: TimerSettings,
    onSettingsChanged: (TimerSettings) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workMinutes by remember { 
        mutableIntStateOf(currentSettings.workDurationMillis.toInt() / (60 * 1000)) 
    }
    var breakSeconds by remember { 
        mutableIntStateOf(currentSettings.breakDurationMillis.toInt() / 1000) 
    }
    var repeatCount by remember { 
        mutableIntStateOf(if (currentSettings.isUnlimitedRepeat()) 0 else currentSettings.repeatCount) 
    }
    var isUnlimited by remember { 
        mutableStateOf(currentSettings.isUnlimitedRepeat()) 
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // タイトル
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "設定",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = onNavigateBack) {
                Text("完了")
            }
        }

        Divider()

        // ワーク時間設定
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ワーク時間",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = workMinutes.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { minutes ->
                                if (minutes in 1..999) {
                                    workMinutes = minutes
                                }
                            }
                        },
                        label = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp)
                    )
                    Text("分間")
                }
            }
        }

        // ブレイク時間設定
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ブレイク時間",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = breakSeconds.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { seconds ->
                                if (seconds in 1..999) {
                                    breakSeconds = seconds
                                }
                            }
                        },
                        label = { Text("秒") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp)
                    )
                    Text("秒間")
                }
            }
        }

        // リピート回数設定
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "繰り返し設定",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isUnlimited,
                        onCheckedChange = { isUnlimited = it }
                    )
                    Text("無制限")
                }
                
                if (!isUnlimited) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = repeatCount.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { count ->
                                    if (count in 1..999) {
                                        repeatCount = count
                                    }
                                }
                            },
                            label = { Text("回数") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(120.dp)
                        )
                        Text("サイクル")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 保存ボタン
        Button(
            onClick = {
                val newSettings = TimerSettings(
                    workDurationMillis = workMinutes * 60 * 1000L,
                    breakDurationMillis = breakSeconds * 1000L,
                    repeatCount = if (isUnlimited) TimerSettings.UNLIMITED_REPEAT else repeatCount
                )
                onSettingsChanged(newSettings)
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("設定を保存")
        }
    }
}