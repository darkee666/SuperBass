package com.example.superbass

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private fun changed() {
        Settings.save(this)
        EffectService.instance?.applyAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.load(this)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        build()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(24))
            setBackgroundColor(Color.parseColor("#121212"))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        fun text(t: String, size: Float = 15f, color: Int = Color.WHITE) = TextView(this).apply {
            this.text = t; textSize = size; setTextColor(color)
            setPadding(0, dp(10), 0, dp(2))
        }

        root.addView(text("СУПЕР БАС", 28f, Color.parseColor("#FF5252")))
        root.addView(text("Улучшение звука для всех плееров", 14f, Color.LTGRAY))

        val sw = Switch(this).apply {
            text = "Включить эффекты"; textSize = 18f
            setTextColor(Color.WHITE); isChecked = Settings.enabled
            setPadding(0, dp(16), 0, dp(8))
            setOnCheckedChangeListener { _, on ->
                Settings.enabled = on
                Settings.save(this@MainActivity)
                val i = Intent(this@MainActivity, EffectService::class.java)
                if (on) startForegroundService(i) else stopService(i)
                if (on) EffectService.instance?.applyAll()
            }
        }
        root.addView(sw)

        fun slider(label: String, min: Int, max: Int, value: Int, unit: String, set: (Int) -> Unit) {
            val tv = text("$label: $value$unit")
            val sb = SeekBar(this).apply {
                this.max = max - min
                progress = value - min
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                        val v = p + min
                        tv.text = "$label: $v$unit"
                        if (fromUser) { set(v); changed() }
                    }
                    override fun onStartTrackingTouch(s: SeekBar) {}
                    override fun onStopTrackingTouch(s: SeekBar) {}
                })
            }
            root.addView(tv); root.addView(sb)
        }

        root.addView(text("Пресеты", 18f, Color.parseColor("#FF5252")))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        fun presetBtn(label: String, key: String) {
            row.addView(Button(this).apply {
                text = label; textSize = 12f
                setOnClickListener { Settings.preset(key); changed(); build() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        presetBtn("Супер бас", "bass"); presetBtn("Чёткий звук", "clear"); presetBtn("Сброс", "flat")
        root.addView(row)

        root.addView(text("Основное", 18f, Color.parseColor("#FF5252")))
        slider("Бас-бустер", 0, 1000, Settings.bass, "") { Settings.bass = it }
        slider("Объёмный звук", 0, 1000, Settings.virt, "") { Settings.virt = it }
        slider("Усиление громкости", 0, 1500, Settings.gain, " мБ") { Settings.gain = it }

        root.addView(text("Эквалайзер", 18f, Color.parseColor("#FF5252")))
        val names = arrayOf("60 Гц", "230 Гц", "910 Гц", "3.6 кГц", "14 кГц")
        for (i in 0..4) slider(names[i], -1500, 1500, Settings.bands[i], " мБ") { Settings.bands[i] = it }

        root.addView(text(
            "Как пользоваться: включите переключатель, затем запустите музыку в любом плеере " +
            "(если музыка уже играет, поставьте её на паузу и включите заново). " +
            "Не все приложения сообщают системе о своём звуке (например, некоторые видеосервисы).\n\n" +
            "Осторожно: большое усиление громкости и баса может хрипеть и портить динамики. " +
            "Начинайте с низкой громкости.", 13f, Color.GRAY))
    }
}
