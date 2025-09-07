package com.example.a20_20_20

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.a20_20_20.service.TimerService
import com.example.a20_20_20.ui.TimerScreen
import com.example.a20_20_20.ui.theme._20_20_20Theme
import com.example.a20_20_20.util.ScreenLockManager
import com.example.a20_20_20.domain.TimerStatus
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    
    private var notificationPermissionDenied by mutableStateOf(false)
    private var exactAlarmPermissionDenied by mutableStateOf(false)
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "通知権限リクエスト結果: $isGranted")
        notificationPermissionDenied = !isGranted
        
        if (!isGranted) {
            // 権限が拒否された場合、もう一度shouldShowRequestPermissionRationaleをチェック
            val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else false
            
            if (!shouldShowRationale) {
                Log.d("MainActivity", "通知権限が永続的に拒否されました。設定画面への誘導が必要です。")
            } else {
                Log.d("MainActivity", "通知権限が一時的に拒否されました。")
            }
        } else {
            Log.d("MainActivity", "通知権限が許可されました。")
        }
    }
    
    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "正確なアラーム権限設定画面から戻りました")
        checkExactAlarmPermission()
        // TimerApplicationの権限状態も更新
        TimerApplication.getInstance().updateExactAlarmPermissionState()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            _20_20_20Theme {
                // 画面ロック制御の監視
                LaunchedEffect(Unit) {
                    val app = TimerApplication.getInstance()
                    app.timerState.collect { timerState ->
                        val notificationSettings = app.notificationSettings.value
                        val isTimerRunning = timerState.status == TimerStatus.RUNNING
                        
                        ScreenLockManager.updateScreenLockSetting(
                            activity = this@MainActivity,
                            keepScreenOn = notificationSettings.keepScreenOnDuringTimer,
                            isTimerRunning = isTimerRunning
                        )
                    }
                }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimerScreen(
                        modifier = Modifier.padding(innerPadding),
                        notificationPermissionDenied = notificationPermissionDenied,
                        exactAlarmPermissionDenied = exactAlarmPermissionDenied,
                        onRetryPermissionRequest = { requestNotificationPermission() },
                        onOpenSettings = { openNotificationSettings() },
                        onRetryExactAlarmPermission = { requestExactAlarmPermission() },
                        onOpenAlarmSettings = { openExactAlarmSettings() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // アプリが表示されるたびに権限をチェック
        checkAndRequestPermissions()
        // TimerApplicationの権限状態も更新
        TimerApplication.getInstance().updateExactAlarmPermissionState()
        // タイマー実行中の場合は通知を復旧
        restoreNotificationIfNeeded()
    }
    
    private fun restoreNotificationIfNeeded() {
        val app = TimerApplication.getInstance()
        val currentState = app.timerState.value
        val service = app.getService()
        
        android.util.Log.d("MainActivity", "Attempting to restore notification for state: ${currentState.status}")
        
        // タイマーが実行中または一時停止中の場合のみ復元処理を実行
        if (currentState.status != com.example.a20_20_20.domain.TimerStatus.STOPPED) {
            if (service != null) {
                // サービスが利用可能な場合は直接復旧メソッドを呼び出し
                android.util.Log.d("MainActivity", "Service available, calling restoreNotification")
                service.restoreNotification()
            } else {
                // サービスが利用できない場合はサービス接続を試行
                android.util.Log.d("MainActivity", "Service not available, starting foreground service")
                val intent = Intent(this, TimerService::class.java)
                try {
                    // フォアグラウンドサービスを開始して通知を復元
                    startForegroundService(intent)
                    android.util.Log.d("MainActivity", "Foreground service started for notification restore")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to start service for notification restore", e)
                    
                    // フォアグラウンドサービス開始に失敗した場合は通常のサービス開始を試行
                    try {
                        startService(intent)
                        android.util.Log.d("MainActivity", "Regular service started as fallback")
                    } catch (fallbackException: Exception) {
                        android.util.Log.e("MainActivity", "Failed to start service even with fallback", fallbackException)
                    }
                }
            }
        } else {
            android.util.Log.d("MainActivity", "Timer is stopped, no notification restoration needed")
        }
    }
    
    private fun checkAndRequestPermissions() {
        requestNotificationPermission()
        checkExactAlarmPermission()
        // バッテリー最適化の除外を要求
        requestBatteryOptimizationExemption()
    }
    
    private fun requestNotificationPermission() {
        // Android 13以降では通知権限が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                Log.d("MainActivity", "通知権限がありません。権限をリクエストします。")
                
                // 権限が拒否されたことがあるかチェック
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                
                if (shouldShowRationale) {
                    Log.d("MainActivity", "権限の説明が必要です。")
                    // ユーザーに権限の必要性を説明してから再度リクエスト
                    notificationPermissionDenied = true
                } else {
                    Log.d("MainActivity", "権限をリクエストします。")
                    // 権限をリクエスト
                    try {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "権限リクエストでエラーが発生しました: ${e.message}")
                        notificationPermissionDenied = true
                    }
                }
            } else {
                Log.d("MainActivity", "通知権限が既に許可されています。")
                notificationPermissionDenied = false
            }
        } else {
            Log.d("MainActivity", "Android 12以前のため通知権限は不要です。")
            // Android 12以前では通知権限は不要
            notificationPermissionDenied = false
        }
    }
    
    private fun checkExactAlarmPermission() {
        // Android 12以降でSCHEDULE_EXACT_ALARM権限が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()
            
            Log.d("MainActivity", "正確なアラーム権限状態: $hasPermission")
            exactAlarmPermissionDenied = !hasPermission
            
            if (!hasPermission) {
                Log.d("MainActivity", "正確なアラーム権限がありません。")
            } else {
                Log.d("MainActivity", "正確なアラーム権限が既に許可されています。")
            }
        } else {
            Log.d("MainActivity", "Android 11以前のため正確なアラーム権限は不要です。")
            exactAlarmPermissionDenied = false
        }
    }
    
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Android 12以降専用のアラーム権限設定画面
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                exactAlarmPermissionLauncher.launch(intent)
                Log.d("MainActivity", "アラーム権限設定画面を開きました")
            } catch (e: Exception) {
                Log.e("MainActivity", "アラーム権限設定画面を開けませんでした: ${e.message}")
                // フォールバック1: アラーム&リマインダー設定画面を試行
                tryOpenAlarmSettings()
            }
        }
    }
    
    private fun tryOpenAlarmSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 特別なアプリアクセス > アラーム&リマインダー設定画面（Android 12以降）
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                Log.d("MainActivity", "特別なアプリアクセス > アラーム&リマインダー設定画面を開きました")
            } else {
                openExactAlarmSettings()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "特別なアプリアクセス設定画面を開けませんでした: ${e.message}")
            // フォールバック: 手動で特別なアプリアクセス画面を開く
            tryOpenSpecialAppAccessSettings()
        }
    }
    
    private fun tryOpenSpecialAppAccessSettings() {
        try {
            // 設定画面のメイン画面を開く
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
            Log.d("MainActivity", "設定画面を開きました")
            
            // ユーザーガイダンスをToastで表示
            Toast.makeText(
                this,
                "アプリ → 特別なアプリアクセス → アラームとリマインダー で許可してください",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "設定画面を開けませんでした: ${e.message}")
            // 最終フォールバック: 通常のアプリ設定画面
            openExactAlarmSettings()
        }
    }
    
    private fun openNotificationSettings() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Log.d("MainActivity", "通知設定画面を開きました")
        } catch (e: Exception) {
            Log.e("MainActivity", "設定画面を開けませんでした: ${e.message}")
        }
    }
    
    private fun openExactAlarmSettings() {
        try {
            // Android 12以降では、システム設定のアプリ詳細画面に移動
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Log.d("MainActivity", "アプリ設定画面を開きました")
            
            // ユーザーガイダンスをToastで表示
            showAlarmPermissionGuidance()
        } catch (e: Exception) {
            Log.e("MainActivity", "アプリ設定画面を開けませんでした: ${e.message}")
        }
    }
    
    private fun showAlarmPermissionGuidance() {
        Toast.makeText(
            this,
            "設定 → アプリ → 特別なアプリアクセス → アラームとリマインダー で許可してください",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // バッテリー最適化設定画面が開けない場合は無視
            }
        }
    }
}

