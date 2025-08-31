package com.example.a20_20_20.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a20_20_20.domain.TimerPhase
import com.example.a20_20_20.domain.TimerStatus
import com.example.a20_20_20.ui.theme._20_20_20Theme

enum class TimeDisplayMode {
    DIGITAL,
    ANALOG
}

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
fun AnalogClock(
    remainingTimeInSeconds: Int,
    totalTimeInSeconds: Int,
    modifier: Modifier = Modifier,
    size: Float = 200f
) {
    val progress = if (totalTimeInSeconds > 0) {
        (totalTimeInSeconds - remainingTimeInSeconds).toFloat() / totalTimeInSeconds
    } else 0f
    
    val minutes = remainingTimeInSeconds / 60
    val seconds = remainingTimeInSeconds % 60
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
        ) {
        val center = Offset(size.dp.toPx() / 2, size.dp.toPx() / 2)
        val radius = size.dp.toPx() / 2 - 20f
        val strokeWidth = 8f
        
        // 背景円
        drawCircle(
            color = Color.Gray.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        
        // 進捗円
        drawArc(
            color = Color.Blue,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // 時計の針（分）
        val minuteAngle = (minutes % 60) * 6f - 90f // 6度 = 360/60
        val minuteHandLength = radius * 0.7f
        val minuteEndX = center.x + cos(Math.toRadians(minuteAngle.toDouble())).toFloat() * minuteHandLength
        val minuteEndY = center.y + sin(Math.toRadians(minuteAngle.toDouble())).toFloat() * minuteHandLength
        
        drawLine(
            color = Color.Black,
            start = center,
            end = Offset(minuteEndX, minuteEndY),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        
        // 時計の針（秒）
        val secondAngle = seconds * 6f - 90f // 6度 = 360/60
        val secondHandLength = radius * 0.9f
        val secondEndX = center.x + cos(Math.toRadians(secondAngle.toDouble())).toFloat() * secondHandLength
        val secondEndY = center.y + sin(Math.toRadians(secondAngle.toDouble())).toFloat() * secondHandLength
        
        drawLine(
            color = Color.Red,
            start = center,
            end = Offset(secondEndX, secondEndY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        
        // 中心点
        drawCircle(
            color = Color.Black,
            radius = 8f,
            center = center
        )
        
        // 時間マーカー
        for (i in 0 until 12) {
            val angle = i * 30f - 90f // 30度間隔
            val markerStart = radius * 0.85f
            val markerEnd = radius * 0.95f
            
            val startX = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * markerStart
            val startY = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * markerStart
            val endX = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * markerEnd
            val endY = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * markerEnd
            
            drawLine(
                color = Color.Black,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (i % 3 == 0) 4f else 2f,
                cap = StrokeCap.Round
            )
        }
        }
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    minTextSize: Float = 12f,
    maxTextSize: Float = 80f
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    BoxWithConstraints(modifier = modifier) {
        val constraintsMaxWidth = this.maxWidth
        val constraintsMaxHeight = this.maxHeight
        val maxWidthPx = with(density) { constraintsMaxWidth.toPx() }
        val maxHeightPx = with(density) { constraintsMaxHeight.toPx() }
        
        var textSize by remember(text, constraintsMaxWidth, constraintsMaxHeight) {
            mutableStateOf(maxTextSize)
        }
        
        LaunchedEffect(text, constraintsMaxWidth, constraintsMaxHeight) {
            var currentSize = maxTextSize
            
            while (currentSize >= minTextSize) {
                val currentStyle = style.copy(fontSize = with(density) { currentSize.sp })
                val textLayoutResult = textMeasurer.measure(
                    text = text,
                    style = currentStyle,
                    constraints = Constraints(
                        maxWidth = maxWidthPx.toInt(),
                        maxHeight = if (maxLines == 1) Constraints.Infinity else maxHeightPx.toInt()
                    ),
                    maxLines = maxLines
                )
                
                if (textLayoutResult.size.width <= maxWidthPx &&
                    (maxLines == Int.MAX_VALUE || textLayoutResult.lineCount <= maxLines) &&
                    textLayoutResult.size.height <= maxHeightPx
                ) {
                    textSize = currentSize
                    break
                }
                
                currentSize -= 2f
            }
            
            if (textSize < minTextSize) {
                textSize = minTextSize
            }
        }
        
        Text(
            text = text,
            style = style.copy(fontSize = with(density) { textSize.sp }),
            maxLines = maxLines,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SwipeableTimeDisplay(
    formattedTime: String,
    remainingTimeInSeconds: Int,
    totalTimeInSeconds: Int,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(TimeDisplayMode.DIGITAL) }
    var dragOffset by remember { mutableStateOf(0f) }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val clockSize = if (isLandscape) 140f else 200f
    val digitalFontSize = if (isLandscape) 48.sp else 88.sp
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100f) {
                            displayMode = if (displayMode == TimeDisplayMode.DIGITAL) {
                                TimeDisplayMode.ANALOG
                            } else {
                                TimeDisplayMode.DIGITAL
                            }
                        } else if (dragOffset < -100f) {
                            displayMode = if (displayMode == TimeDisplayMode.ANALOG) {
                                TimeDisplayMode.DIGITAL
                            } else {
                                TimeDisplayMode.ANALOG
                            }
                        }
                        dragOffset = 0f
                    }
                ) { _, dragAmount ->
                    dragOffset += dragAmount
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (displayMode) {
            TimeDisplayMode.DIGITAL -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AutoSizeText(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        maxTextSize = if (isLandscape) 60f else 100f,
                        minTextSize = 24f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            TimeDisplayMode.ANALOG -> {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val availableWidth = this.maxWidth
                    val availableHeight = this.maxHeight
                    
                    if (isLandscape) {
                        val clockSize = min(availableWidth.value * 0.6f, availableHeight.value * 0.8f)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnalogClock(
                                remainingTimeInSeconds = remainingTimeInSeconds,
                                totalTimeInSeconds = totalTimeInSeconds,
                                size = clockSize
                            )
                        }
                    } else {
                        val clockSize = min(availableWidth.value * 0.8f, availableHeight.value * 0.6f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnalogClock(
                                remainingTimeInSeconds = remainingTimeInSeconds,
                                totalTimeInSeconds = totalTimeInSeconds,
                                size = clockSize
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // スワイプヒント
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == displayMode.ordinal) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
                        )
                )
            }
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    if (isLandscape) {
        // 横向き時のレイアウト
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 左側：時間表示エリア
            Box(
                modifier = Modifier.weight(1.2f),
                contentAlignment = Alignment.Center
            ) {
                // 残り時間表示
                SwipeableTimeDisplay(
                    formattedTime = uiState.formattedTime,
                    remainingTimeInSeconds = uiState.remainingTimeInSeconds,
                    totalTimeInSeconds = uiState.totalTimeInSeconds,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 右側：情報とコントロールエリア
            Column(
                modifier = Modifier.weight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 設定ボタン
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "設定"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // フェーズ表示
                Text(
                    text = uiState.phaseLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // サイクル情報表示
                Text(
                    text = uiState.cycleInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // コントロールボタン
                TimerControls(
                    status = uiState.timerState.status,
                    onStartClick = onStartClick,
                    onPauseClick = onPauseClick,
                    onStopClick = onStopClick
                )
            }
        }
    } else {
        // 縦向き時のレイアウト
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
            SwipeableTimeDisplay(
                formattedTime = uiState.formattedTime,
                remainingTimeInSeconds = uiState.remainingTimeInSeconds,
                totalTimeInSeconds = uiState.totalTimeInSeconds,
                modifier = Modifier.padding(horizontal = 16.dp)
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

@Preview(showBackground = true)
@Composable
fun AnalogClockPreview() {
    _20_20_20Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("アナログ時計表示", style = MaterialTheme.typography.headlineSmall)
            AnalogClock(
                remainingTimeInSeconds = 300, // 5分
                totalTimeInSeconds = 1200, // 20分
                size = 200f
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SwipeableTimeDisplayPreview() {
    _20_20_20Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("スワイプ対応時間表示", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            SwipeableTimeDisplay(
                formattedTime = "05:00",
                remainingTimeInSeconds = 300,
                totalTimeInSeconds = 1200,
                modifier = Modifier.height(280.dp)
            )
            Text(
                "左右にスワイプして表示を切り替え",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 640,
    heightDp = 360,
    name = "横表示 - デジタル"
)
@Composable
fun TimerContentLandscapeDigitalPreview() {
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

@Preview(
    showBackground = true,
    widthDp = 640,
    heightDp = 360,
    name = "横表示 - アナログ"
)
@Composable
fun TimerContentLandscapeAnalogPreview() {
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