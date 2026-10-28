package com.example.superbass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

class SessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Settings.load(context)
        if (!Settings.enabled) return
        val svc = EffectService.instance
        val session = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        if (svc != null) {
            svc.handle(intent.action, session)
        } else {
            try {
                val i = Intent(context, EffectService::class.java)
                i.putExtra("action", intent.action)
                i.putExtra("session", session)
                context.startForegroundService(i)
            } catch (_: Exception) { }
        }
    }
}
