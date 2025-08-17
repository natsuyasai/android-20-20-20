package com.example.a20_20_20.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.a20_20_20.domain.NotificationSettings
import com.example.a20_20_20.domain.TimerSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: TimerSettings,
    currentNotificationSettings: NotificationSettings = NotificationSettings.DEFAULT,
    onSettingsChanged: (TimerSettings) -> Unit,
    onNotificationSettingsChanged: (NotificationSettings) -> Unit = {},
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
    
    // 通知設定の状態
    var enableSound by remember { mutableStateOf(currentNotificationSettings.enableSound) }
    var enableVibration by remember { mutableStateOf(currentNotificationSettings.enableVibration) }
    var soundVolume by remember { mutableFloatStateOf(currentNotificationSettings.soundVolume) }
    var workCompleteSound by remember { mutableStateOf(currentNotificationSettings.workCompleteSound) }
    var breakCompleteSound by remember { mutableStateOf(currentNotificationSettings.breakCompleteSound) }
    
    val context = LocalContext.current
    
    // 通知音選択用ランチャー
    val workSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            workCompleteSound = uri
        }
    }
    
    val breakSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            breakCompleteSound = uri
        }
    }

    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isUnlimited = !isUnlimited }
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

        // 通知設定
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "通知設定",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // サウンド有効/無効
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { enableSound = !enableSound }
                ) {
                    Checkbox(
                        checked = enableSound,
                        onCheckedChange = { enableSound = it }
                    )
                    Text("通知音を有効にする")
                }
                
                if (enableSound) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 音量調整
                    Text("音量: ${(soundVolume * 100).toInt()}%")
                    Slider(
                        value = soundVolume,
                        onValueChange = { soundVolume = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ワーク完了音選択
                    Button(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "ワーク完了音を選択")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, workCompleteSound)
                            }
                            workSoundLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ワーク完了音を選択")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ブレイク完了音選択
                    Button(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "ブレイク完了音を選択")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, breakCompleteSound)
                            }
                            breakSoundLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ブレイク完了音を選択")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // バイブレーション設定
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { enableVibration = !enableVibration }
                ) {
                    Checkbox(
                        checked = enableVibration,
                        onCheckedChange = { enableVibration = it }
                    )
                    Text("バイブレーションを有効にする")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 保存ボタン
        Button(
            onClick = {
                val newSettings = TimerSettings(
                    workDurationMillis = workMinutes * 60 * 1000L,
                    breakDurationMillis = breakSeconds * 1000L,
                    repeatCount = if (isUnlimited) TimerSettings.UNLIMITED_REPEAT else repeatCount
                )
                val newNotificationSettings = NotificationSettings(
                    workCompleteSound = workCompleteSound,
                    breakCompleteSound = breakCompleteSound,
                    enableSound = enableSound,
                    enableVibration = enableVibration,
                    soundVolume = soundVolume
                )
                onSettingsChanged(newSettings)
                onNotificationSettingsChanged(newNotificationSettings)
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("設定を保存")
        }
    }
}