package com.example.a20_20_20.util

import android.app.Activity
import android.view.WindowManager

/**
 * 画面の自動ロックを制御するユーティリティクラス
 */
class ScreenLockManager {
    
    companion object {
        /**
         * 画面の自動ロックを無効にする
         * @param activity 対象のActivity
         */
        fun keepScreenOn(activity: Activity) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        /**
         * 画面の自動ロック無効を解除する
         * @param activity 対象のActivity
         */
        fun allowScreenOff(activity: Activity) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        /**
         * 設定に基づいて画面ロック制御を適用する
         * @param activity 対象のActivity
         * @param keepScreenOn 画面を点灯し続けるかどうか
         * @param isTimerRunning タイマーが動作中かどうか
         */
        fun updateScreenLockSetting(activity: Activity, keepScreenOn: Boolean, isTimerRunning: Boolean) {
            if (keepScreenOn && isTimerRunning) {
                keepScreenOn(activity)
            } else {
                allowScreenOff(activity)
            }
        }
    }
}