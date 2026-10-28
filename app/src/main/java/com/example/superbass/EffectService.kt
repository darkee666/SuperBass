package com.example.superbass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.IBinder

class EffectService : Service() {

    companion object {
        @Volatile var instance: EffectService? = null
    }

    private class Chain(session: Int) {
        var eq: Equalizer? = try { Equalizer(0, session) } catch (_: Throwable) { null }
        var bb: BassBoost? = try { BassBoost(0, session) } catch (_: Throwable) { null }
        var vz: Virtualizer? = try { Virtualizer(0, session) } catch (_: Throwable) { null }
        var le: LoudnessEnhancer? = try { LoudnessEnhancer(session) } catch (_: Throwable) { null }

        fun apply() {
            val on = Settings.enabled
            try {
                eq?.let { e ->
                    val n = e.numberOfBands.toInt()
                    val r = e.bandLevelRange
                    for (i in 0 until n) {
                        val idx = if (n == 1) 0 else Math.round(i * 4.0 / (n - 1)).toInt()
                        val lvl = Settings.bands[idx].coerceIn(r[0].toInt(), r[1].toInt())
                        e.setBandLevel(i.toShort(), lvl.toShort())
                    }
                    e.enabled = on
                }
            } catch (_: Throwable) { }
            try {
                bb?.let {
                    if (it.strengthSupported) it.setStrength(Settings.bass.toShort())
                    it.enabled = on && Settings.bass > 0
                }
            } catch (_: Throwable) { }
            try {
                vz?.let {
                    if (it.strengthSupported) it.setStrength(Settings.virt.toShort())
                    it.enabled = on && Settings.virt > 0
                }
            } catch (_: Throwable) { }
            try {
                le?.let {
                    it.setTargetGain(Settings.gain)
                    it.enabled = on && Settings.gain > 0
                }
            } catch (_: Throwable) { }
        }

        fun release() {
            try { eq?.release() } catch (_: Throwable) { }
            try { bb?.release() } catch (_: Throwable) { }
            try { vz?.release() } catch (_: Throwable) { }
            try { le?.release() } catch (_: Throwable) { }
        }
    }

    private val chains = HashMap<Int, Chain>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            handle(i.action, i.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1))
        }
    }

    @Synchronized
    fun handle(action: String?, session: Int) {
        if (session < 0) return
        if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
            if (!chains.containsKey(session)) {
                val c = Chain(session)
                c.apply()
                chains[session] = c
            }
        } else if (action == AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) {
            chains.remove(session)?.release()
        }
    }

    @Synchronized
    fun applyAll() {
        chains.values.forEach { it.apply() }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Settings.load(this)
        val f = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_EXPORTED)
        else registerReceiver(receiver, f)

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("fx", "Эффекты звука", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "fx")
            .setContentTitle("Супер Бас работает")
            .setContentText("Улучшение звука включено")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .build()
        startForeground(1, n)

        // Попытка повесить эффекты на глобальный микс (работает не на всех телефонах)
        handle(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION, 0)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val a = it.getStringExtra("action")
            val s = it.getIntExtra("session", -1)
            if (a != null) handle(a, s)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        synchronized(this) {
            chains.values.forEach { it.release() }
            chains.clear()
        }
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
