package com.example.a20_20_20.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TimerAlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerAlarmReceiver", "Received alarm broadcast: ${intent.action}")
        
        when (intent.action) {
            TimerEngine.ACTION_PHASE_COMPLETE -> {
                // フェーズ完了の処理をTimerServiceに委譲
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_PHASE_COMPLETE
                }
                
                try {
                    context.startForegroundService(serviceIntent)
                    Log.d("TimerAlarmReceiver", "Phase complete event sent to TimerService")
                } catch (e: Exception) {
                    Log.e("TimerAlarmReceiver", "Failed to start TimerService for phase completion", e)
                }
            }
            TimerEngine.ACTION_TIMER_TICK -> {
                // UI更新のトリガー（将来的に使用する可能性）
                Log.d("TimerAlarmReceiver", "Timer tick received")
            }
            else -> {
                Log.w("TimerAlarmReceiver", "Unknown action received: ${intent.action}")
            }
        }
    }
}