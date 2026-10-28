package com.example.superbass

import android.content.Context

object Settings {
    var enabled = false
    var bass = 1000      // 0..1000
    var virt = 400       // 0..1000
    var gain = 600       // 0..1500 мБ (громкость)
    val bands = intArrayOf(1500, 1000, -200, 0, 300) // мБ, 5 полос

    fun load(c: Context) {
        val p = c.getSharedPreferences("s", Context.MODE_PRIVATE)
        enabled = p.getBoolean("en", false)
        bass = p.getInt("bass", bass)
        virt = p.getInt("virt", virt)
        gain = p.getInt("gain", gain)
        for (i in bands.indices) bands[i] = p.getInt("b$i", bands[i])
    }

    fun save(c: Context) {
        val e = c.getSharedPreferences("s", Context.MODE_PRIVATE).edit()
        e.putBoolean("en", enabled).putInt("bass", bass).putInt("virt", virt).putInt("gain", gain)
        for (i in bands.indices) e.putInt("b$i", bands[i])
        e.apply()
    }

    fun preset(name: String) {
        val b: IntArray
        when (name) {
            "bass" -> { bass = 1000; virt = 400; gain = 600; b = intArrayOf(1500, 1000, -200, 0, 300) }
            "clear" -> { bass = 300; virt = 600; gain = 300; b = intArrayOf(300, 0, 200, 500, 600) }
            else -> { bass = 0; virt = 0; gain = 0; b = intArrayOf(0, 0, 0, 0, 0) }
        }
        for (i in bands.indices) bands[i] = b[i]
    }
}
