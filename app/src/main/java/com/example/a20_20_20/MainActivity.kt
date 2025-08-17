package com.example.a20_20_20

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
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
import com.example.a20_20_20.ui.TimerScreen
import com.example.a20_20_20.ui.theme._20_20_20Theme

class MainActivity : ComponentActivity() {
    
    private var notificationPermissionDenied by mutableStateOf(false)
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "権限リクエスト結果: $isGranted")
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
                Log.d("MainActivity", "権限が永続的に拒否されました。設定画面への誘導が必要です。")
                // 「今後表示しない」が選択された場合、設定画面へ誘導
            } else {
                Log.d("MainActivity", "権限が一時的に拒否されました。")
            }
        } else {
            Log.d("MainActivity", "通知権限が許可されました。")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            _20_20_20Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimerScreen(
                        modifier = Modifier.padding(innerPadding),
                        notificationPermissionDenied = notificationPermissionDenied,
                        onRetryPermissionRequest = { requestNotificationPermission() },
                        onOpenSettings = { openNotificationSettings() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // アプリが表示されるたびに権限をチェック
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
        requestNotificationPermission()
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

