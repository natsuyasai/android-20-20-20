package com.example.a20_20_20.ui

import androidx.compose.foundation.layout.*
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
    viewModel: TimerViewModel = viewModel(factory = TimerViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    
    TimerContent(
        modifier = modifier,
        uiState = uiState,
        onStartClick = { viewModel.startTimer() },
        onPauseClick = { viewModel.pauseTimer() },
        onStopClick = { viewModel.stopTimer() }
    )
}

@Composable
fun TimerContent(
    modifier: Modifier = Modifier,
    uiState: TimerUiState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

@Preview(showBackground = true)
@Composable
fun TimerScreenPreview() {
    _20_20_20Theme {
        TimerContent(
            uiState = TimerUiState(),
            onStartClick = {},
            onPauseClick = {},
            onStopClick = {}
        )
    }
}