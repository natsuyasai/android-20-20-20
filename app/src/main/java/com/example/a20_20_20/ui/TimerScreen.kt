package com.example.a20_20_20.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerStatus
import com.example.a20_20_20.ui.theme._20_20_20Theme

@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = viewModel(factory = TimerViewModelFactory()),
    notificationPermissionDenied: Boolean = false,
    exactAlarmPermissionDenied: Boolean = false,
    onRetryPermissionRequest: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onRetryExactAlarmPermission: () -> Unit = {},
    onOpenAlarmSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val notificationSettings by viewModel.notificationSettings.collectAsState()
    val timerSettings by viewModel.timerSettings.collectAsState()
    
    if (showSettings) {
        SettingsScreen(
            currentSettings = timerSettings,
            currentNotificationSettings = notificationSettings,
            onSettingsChanged = { settings ->
                viewModel.updateSettings(settings)
            },
            onNotificationSettingsChanged = { settings ->
                viewModel.updateNotificationSettings(settings)
            },
            onNavigateBack = { viewModel.navigateBack() },
            modifier = modifier
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            // 通知権限拒否メッセージ
            if (notificationPermissionDenied) {
                NotificationPermissionBanner(
                    onRetryClick = onRetryPermissionRequest,
                    onOpenSettingsClick = onOpenSettings
                )
            }
            
            // 正確なアラーム権限拒否メッセージ
            if (exactAlarmPermissionDenied) {
                ExactAlarmPermissionBanner(
                    onRetryClick = onRetryExactAlarmPermission,
                    onOpenSettingsClick = onOpenAlarmSettings
                )
            }
            
            TimerContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onStartClick = { viewModel.startTimer() },
                onPauseClick = { viewModel.pauseTimer() },
                onStopClick = { viewModel.stopTimer() },
                onSettingsClick = { viewModel.navigateToSettings() }
            )
        }
    }
}

@Composable
fun NotificationPermissionBanner(
    onRetryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "警告",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "通知権限が必要です",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "タイマーの通知を表示するために権限が必要です。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetryClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("再試行")
                }
                
                Button(
                    onClick = onOpenSettingsClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                        contentColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text("設定を開く")
                }
            }
        }
    }
}

@Composable
fun TimerContent(
    modifier: Modifier = Modifier,
    uiState: TimerUiState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 設定ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "設定"
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // フェーズ表示
        Text(
            text = uiState.phaseLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 残り時間表示
        Text(
            text = uiState.formattedTime,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // サイクル情報表示
        Text(
            text = uiState.cycleInfo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // コントロールボタン
        TimerControls(
            status = uiState.timerState.status,
            onStartClick = onStartClick,
            onPauseClick = onPauseClick,
            onStopClick = onStopClick
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun TimerControls(
    status: TimerStatus,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (status) {
            TimerStatus.STOPPED -> {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("開始")
                }
            }
            TimerStatus.RUNNING -> {
                Button(
                    onClick = onPauseClick,
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("一時停止")
                }
                Button(
                    onClick = onStopClick,
                    modifier = Modifier.width(120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("停止")
                }
            }
            TimerStatus.PAUSED -> {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("再開")
                }
                Button(
                    onClick = onStopClick,
                    modifier = Modifier.width(120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("停止")
                }
            }
        }
    }
}

@Composable
fun ExactAlarmPermissionBanner(
    onRetryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "正確なアラーム権限が必要です",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "20-20-20タイマーが正確に動作するために、アラーム権限を許可してください。この権限により、バックグラウンドでも正確なタイミングで通知を表示できます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "手順:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "設定 → アプリ → 特別なアプリアクセス → アラームとリマインダー",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onRetryClick) {
                    Text(
                        text = "アラーム権限設定",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                TextButton(onClick = onOpenSettingsClick) {
                    Text(
                        text = "手動で設定",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreview() {
    _20_20_20Theme {
        TimerContent(
            uiState = TimerUiState(),
            onStartClick = {},
            onPauseClick = {},
            onStopClick = {},
            onSettingsClick = {}
        )
    }
}