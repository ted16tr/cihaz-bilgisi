package com.example.cihazbilgisi

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = bilgiler()
            textSize = 16f
            setPadding(40, 100, 40, 40)
            setTextIsSelectable(true)
        }
        setContentView(ScrollView(this).apply { addView(tv) })
    }

    private fun gb(b: Long) = String.format("%.2f GB", b / 1073741824.0)

    private fun bilgiler(): String {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        val st = StatFs(Environment.getDataDirectory().path)
        val dm = resources.displayMetrics

        val bat = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = bat?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val sarjda = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val sicaklik = (bat?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0

        val donanim = try {
            File("/proc/cpuinfo").readLines()
                .firstOrNull { it.startsWith("Hardware") }
                ?.substringAfter(":")?.trim()
        } catch (e: Exception) { null }

        val soc = if (Build.VERSION.SDK_INT >= 31)
            "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}" else "-"

        return buildString {
            appendLine("📱 CİHAZ")
            appendLine("Marka: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Cihaz kodu: ${Build.DEVICE}")
            appendLine()
            appendLine("🤖 YAZILIM")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Güvenlik yaması: ${Build.VERSION.SECURITY_PATCH}")
            appendLine("Build: ${Build.DISPLAY}")
            appendLine()
            appendLine("⚙️ İŞLEMCİ")
            appendLine("Çekirdek sayısı: ${Runtime.getRuntime().availableProcessors()}")
            appendLine("Mimari: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Donanım: ${donanim ?: Build.HARDWARE}")
            appendLine("SoC: $soc")
            appendLine()
            appendLine("🧠 RAM")
            appendLine("Toplam: ${gb(mi.totalMem)}")
            appendLine("Boş: ${gb(mi.availMem)}")
            appendLine()
            appendLine("💾 DEPOLAMA")
            appendLine("Toplam: ${gb(st.totalBytes)}")
            appendLine("Boş: ${gb(st.availableBytes)}")
            appendLine()
            appendLine("🖥️ EKRAN")
            appendLine("Çözünürlük: ${dm.widthPixels} x ${dm.heightPixels}")
            appendLine("Yoğunluk: ${dm.densityDpi} dpi")
            appendLine()
            appendLine("🔋 PİL")
            appendLine("Seviye: %${level * 100 / scale}")
            appendLine("Durum: ${if (sarjda) "Şarj oluyor" else "Şarjda değil"}")
            appendLine("Sıcaklık: $sicaklik °C")
        }
    }
}
