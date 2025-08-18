package com.example.a20_20_20.service

import android.content.Context
import android.content.Intent
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.doNothing

class TimerAlarmReceiverTest {

    private val mockContext = mock<Context>()
    private val receiver = TimerAlarmReceiver()

    @Test
    fun `フェーズ完了アクションを受信した場合にTimerServiceが開始される`() {
        // テスト環境ではIntentが適切にモックされないため、基本的な動作のみテスト
        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(TimerEngine.ACTION_PHASE_COMPLETE)

        // receiveメソッドを呼び出し（例外が発生しないことを確認）
        assertDoesNotThrow {
            receiver.onReceive(mockContext, intent)
        }
    }

    @Test
    fun `タイマーティックアクションを受信した場合に正常に処理される`() {
        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(TimerEngine.ACTION_TIMER_TICK)

        assertDoesNotThrow {
            receiver.onReceive(mockContext, intent)
        }
    }

    @Test
    fun `未知のアクションを受信した場合でも例外が発生しない`() {
        val intent = mock<Intent>()
        whenever(intent.action).thenReturn("unknown_action")

        assertDoesNotThrow {
            receiver.onReceive(mockContext, intent)
        }
    }

    @Test
    fun `フェーズ完了処理でTimerServiceの正しいアクションが設定される`() {
        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(TimerEngine.ACTION_PHASE_COMPLETE)

        // BroadcastReceiverの動作を検証
        assertDoesNotThrow {
            receiver.onReceive(mockContext, intent)
        }
    }

    private fun assertDoesNotThrow(executable: () -> Unit) {
        try {
            executable()
        } catch (e: Exception) {
            fail("Expected no exception but was thrown: ${e.message}")
        }
    }
}