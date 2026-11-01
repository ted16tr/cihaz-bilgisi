package com.example.cihazbilgisi

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : Activity() {

    private val bg = Color.parseColor("#0F1117")
    private val kartRenk = Color.parseColor("#1A1D27")
    private val yaziAna = Color.parseColor("#FFFFFF")
    private val yaziSoluk = Color.parseColor("#8B90A0")
    private val vurgu = Color.parseColor("#6C8CFF")
    private val izRenk = Color.parseColor("#2A2E3D")

    private lateinit var icerik: LinearLayout
    private lateinit var tabSatiri: LinearLayout
    private lateinit var kaydirma: ScrollView
    private val sekmeler = ArrayList<Pair<String, () -> Unit>>()
    private var secili = 0

    // ---------- yardımcılar ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun gb(b: Long) = String.format("%.2f GB", b / 1073741824.0)

    private fun yuvarlak(renk: Int, r: Int) = GradientDrawable().apply {
        setColor(renk)
        cornerRadius = dp(r).toFloat()
    }

    private fun barRengi(yuzde: Int) = when {
        yuzde >= 85 -> Color.parseColor("#FF5C5C")
        yuzde >= 60 -> Color.parseColor("#FFB347")
        else -> Color.parseColor("#4ADE80")
    }

    private fun oku(yol: String): String? = try {
        File(yol).readText().trim().ifEmpty { null }
    } catch (e: Exception) { null }

    private fun prop(ad: String): String? = try {
        val p = Runtime.getRuntime().exec(arrayOf("getprop", ad))
        p.inputStream.bufferedReader().readText().trim().ifEmpty { null }
    } catch (e: Exception) { null }

    private fun pazarlamaAdi(): String? {
        val anahtarlar = listOf(
            "ro.product.marketname", "ro.product.vendor.marketname",
            "ro.product.odm.marketname", "ro.config.marketing_name",
            "ro.vendor.oplus.market.name", "ro.oppo.market.name"
        )
        for (a in anahtarlar) prop(a)?.let { return it }
        return null
    }

    private fun evetHayir(b: Boolean) = if (b) "Var ✓" else "Yok"

    // ---------- arayüz parçaları ----------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val ana = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        ana.addView(TextView(this).apply {
            text = "Cihaz Bilgisi"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(yaziAna)
            setPadding(dp(20), dp(20), dp(20), 0)
        })
        ana.addView(TextView(this).apply {
            text = pazarlamaAdi()
                ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            textSize = 14f
            setTextColor(yaziSoluk)
            setPadding(dp(20), dp(2), dp(20), dp(4))
        })

        tabSatiri = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        ana.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabSatiri)
        })

        icerik = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(32))
        }
        kaydirma = ScrollView(this).apply { addView(icerik) }
        ana.addView(kaydirma, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        setContentView(ana)

        sekmeler.add("📱 Cihaz" to { sekmeCihaz() })
        sekmeler.add("🤖 Sistem" to { sekmeSistem() })
        sekmeler.add("⚙️ İşlemci" to { sekmeIslemci() })
        sekmeler.add("🧠 Bellek" to { sekmeBellek() })
        sekmeler.add("🖥️ Ekran" to { sekmeEkran() })
        sekmeler.add("🔋 Pil" to { sekmePil() })
        sekmeler.add("📷 Kamera" to { sekmeKamera() })
        sekmeler.add("📡 Sensörler" to { sekmeSensor() })
        sekmeler.add("🌐 Ağ" to { sekmeAg() })
        sekmeler.add("✨ Özellikler" to { sekmeOzellik() })

        sec(0)
    }

    private fun sec(i: Int) {
        secili = i
        icerik.removeAllViews()
        try {
            sekmeler[i].second()
        } catch (e: Exception) {
            val k = kart("⚠️", "HATA")
            satir(k, "Bu sekme yüklenemedi", e.message)
        }
        kaydirma.scrollTo(0, 0)
        tabCiz()
    }

    private fun tabCiz() {
        tabSatiri.removeAllViews()
        for ((i, s) in sekmeler.withIndex()) {
            val aktif = i == secili
            val tv = TextView(this).apply {
                text = s.first
                textSize = 14f
                setTypeface(null, if (aktif) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (aktif) Color.WHITE else yaziSoluk)
                background = yuvarlak(if (aktif) vurgu else kartRenk, 20)
                setPadding(dp(16), dp(9), dp(16), dp(9))
                setOnClickListener { sec(i) }
            }
            tabSatiri.addView(tv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                rightMargin = dp(8)
            })
        }
    }

    private fun kart(emoji: String, baslik: String): LinearLayout {
        val k = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = yuvarlak(kartRenk, 20)
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        k.addView(TextView(this).apply {
            text = "$emoji  $baslik"
            textSize = 13f
            letterSpacing = 0.08f
            setTypeface(null, Typeface.BOLD)
            setTextColor(vurgu)
            setPadding(0, 0, 0, dp(8))
        })
        icerik.addView(k, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(12)
        })
        return k
    }

    private fun satir(k: LinearLayout, etiket: String, deger: String?) {
        val s = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }
        s.addView(TextView(this).apply {
            text = etiket
            textSize = 14f
            setTextColor(yaziSoluk)
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = dp(8) })
        s.addView(TextView(this).apply {
            text = if (deger.isNullOrBlank()) "—" else deger
            textSize = 14f
            gravity = Gravity.END
            setTextColor(yaziAna)
            setTextIsSelectable(true)
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.4f))
        k.addView(s)
    }

    private fun bar(k: LinearLayout, yuzde: Int, renk: Int) {
        val y = yuzde.coerceIn(0, 100)
        val iz = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 100f
            background = yuvarlak(izRenk, 8)
        }
        val dolu = View(this).apply { background = yuvarlak(renk, 8) }
        iz.addView(dolu, LinearLayout.LayoutParams(0, MATCH_PARENT, y.toFloat()))
        iz.addView(View(this), LinearLayout.LayoutParams(0, MATCH_PARENT, (100 - y).toFloat()))
        k.addView(iz, LinearLayout.LayoutParams(MATCH_PARENT, dp(10)).apply {
            topMargin = dp(8)
            bottomMargin = dp(4)
        })
    }

    // ---------- SEKME: CİHAZ ----------

    private fun sekmeCihaz() {
        val adi = try { Settings.Global.getString(contentResolver, "device_name") } catch (e: Exception) { null }
        val sku = if (Build.VERSION.SDK_INT >= 31) Build.SKU else null
        val odmSku = if (Build.VERSION.SDK_INT >= 31) Build.ODM_SKU else null

        kart("📱", "KİMLİK").also { k ->
            satir(k, "Pazarlama adı", pazarlamaAdi() ?: "Sistem vermiyor")
            satir(k, "Cihaz adı (ayarlardaki)", adi)
            satir(k, "Marka", Build.BRAND)
            satir(k, "Üretici", Build.MANUFACTURER)
            satir(k, "Model kodu", Build.MODEL)
            satir(k, "Cihaz kod adı", Build.DEVICE)
            satir(k, "Ürün", Build.PRODUCT)
        }
        kart("🔧", "DONANIM").also { k ->
            satir(k, "Anakart", Build.BOARD)
            satir(k, "Donanım", Build.HARDWARE)
            satir(k, "Bootloader", Build.BOOTLOADER)
            satir(k, "SKU", sku)
            satir(k, "ODM SKU", odmSku)
            satir(k, "Radyo/Modem", Build.getRadioVersion())
        }
    }

    // ---------- SEKME: SİSTEM ----------

    private fun sekmeSistem() {
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val up = SystemClock.elapsedRealtime() / 1000
        val upTxt = "${up / 86400} gün ${(up % 86400) / 3600} sa ${(up % 3600) / 60} dk"
        val rootYollari = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/data/local/xbin/su"
        )
        val root = rootYollari.any { File(it).exists() }

        kart("🤖", "ANDROID").also { k ->
            satir(k, "Sürüm", Build.VERSION.RELEASE)
            satir(k, "API seviyesi", "${Build.VERSION.SDK_INT}")
            satir(k, "Kod adı", Build.VERSION.CODENAME)
            satir(k, "Güvenlik yaması", Build.VERSION.SECURITY_PATCH)
            satir(k, "Build numarası", Build.DISPLAY)
            satir(k, "Build ID", Build.ID)
            satir(k, "Build tipi", Build.TYPE)
            satir(k, "Build tarihi", fmt.format(Date(Build.TIME)))
        }
        kart("🐧", "ÇEKİRDEK & ÇALIŞMA ZAMANI").also { k ->
            satir(k, "Kernel", System.getProperty("os.version"))
            satir(k, "Mimari", System.getProperty("os.arch"))
            satir(k, "Java VM", System.getProperty("java.vm.version"))
            satir(k, "Açık kalma süresi", upTxt)
        }
        kart("🔐", "GÜVENLİK & YAPI").also { k ->
            satir(k, "Root", if (root) "Tespit edildi ⚠️" else "Tespit edilmedi")
            satir(k, "Verified boot", prop("ro.boot.verifiedbootstate"))
            satir(k, "Treble", prop("ro.treble.enabled"))
            satir(k, "A/B güncelleme", prop("ro.build.ab_update"))
            satir(k, "Dinamik bölümler", prop("ro.boot.dynamic_partitions"))
        }
        kart("🌍", "BÖLGE").also { k ->
            satir(k, "Dil", Locale.getDefault().toString())
            satir(k, "Saat dilimi", TimeZone.getDefault().id)
        }
        kart("🧾", "FINGERPRINT").also { k ->
            satir(k, "Build fingerprint", Build.FINGERPRINT)
        }
    }

    // ---------- SEKME: İŞLEMCİ ----------

    private fun gpuBilgisi(): Triple<String?, String?, String?> {
        return try {
            val d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val v = IntArray(2)
            EGL14.eglInitialize(d, v, 0, v, 1)
            val cfgAttr = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
            )
            val cfgs = arrayOfNulls<EGLConfig>(1)
            val n = IntArray(1)
            EGL14.eglChooseConfig(d, cfgAttr, 0, cfgs, 0, 1, n, 0)
            val ctx = EGL14.eglCreateContext(
                d, cfgs[0], EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
            )
            val surf = EGL14.eglCreatePbufferSurface(
                d, cfgs[0],
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0
            )
            EGL14.eglMakeCurrent(d, surf, surf, ctx)
            val r = GLES20.glGetString(GLES20.GL_RENDERER)
            val ven = GLES20.glGetString(GLES20.GL_VENDOR)
            val ver = GLES20.glGetString(GLES20.GL_VERSION)
            EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(d, surf)
            EGL14.eglDestroyContext(d, ctx)
            EGL14.eglTerminate(d)
            Triple(r, ven, ver)
        } catch (e: Exception) {
            Triple(null, null, null)
        }
    }

    private fun sekmeIslemci() {
        val cpu = try { File("/proc/cpuinfo").readLines() } catch (e: Exception) { emptyList() }
        fun al(k: String) = cpu.firstOrNull { it.startsWith(k) }?.substringAfter(":")?.trim()

        val soc = if (Build.VERSION.SDK_INT >= 31)
            "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}" else null
        val n = Runtime.getRuntime().availableProcessors()

        kart("⚙️", "İŞLEMCİ").also { k ->
            satir(k, "SoC", soc)
            satir(k, "Donanım", al("Hardware") ?: Build.HARDWARE)
            satir(k, "Model adı", al("model name"))
            satir(k, "Çekirdek sayısı", "$n")
            satir(k, "Mimari", Build.SUPPORTED_ABIS.joinToString(", "))
            satir(k, "32-bit ABI", Build.SUPPORTED_32_BIT_ABIS.joinToString(", "))
            satir(k, "64-bit ABI", Build.SUPPORTED_64_BIT_ABIS.joinToString(", "))
            satir(k, "CPU mimari sürümü", al("CPU architecture"))
            satir(k, "BogoMIPS", al("BogoMIPS"))
            satir(k, "Governor", oku("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"))
            satir(k, "Özellikler", al("Features") ?: al("flags"))
        }

        kart("📈", "ÇEKİRDEK HIZLARI").also { k ->
            for (i in 0 until n) {
                val base = "/sys/devices/system/cpu/cpu$i/cpufreq"
                val max = oku("$base/cpuinfo_max_freq")?.toLongOrNull()
                val min = oku("$base/cpuinfo_min_freq")?.toLongOrNull()
                val cur = oku("$base/scaling_cur_freq")?.toLongOrNull()
                val txt = if (max != null) {
                    "${(min ?: 0) / 1000}–${max / 1000} MHz" +
                            (if (cur != null) "\nŞu an: ${cur / 1000} MHz" else "")
                } else "Okunamadı (Android izin vermiyor)"
                satir(k, "Çekirdek $i", txt)
            }
        }

        val (render, vendor, glSurum) = gpuBilgisi()
        val ci = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo
        val vulkanSurum = packageManager.systemAvailableFeatures
            .firstOrNull { it.name == "android.hardware.vulkan.version" }?.version
        val vulkanTxt = vulkanSurum?.let {
            "${(it shr 22)}.${(it shr 12) and 0x3ff}"
        } ?: "Yok"

        kart("🎮", "GPU").also { k ->
            satir(k, "GPU", render)
            satir(k, "Üretici", vendor)
            satir(k, "OpenGL sürümü", glSurum)
            satir(k, "OpenGL ES (cihaz)", ci.glEsVersion)
            satir(k, "Vulkan", vulkanTxt)
        }
    }

    // ---------- SEKME: BELLEK ----------

    private fun sekmeBellek() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        val meminfo = try { File("/proc/meminfo").readLines() } catch (e: Exception) { emptyList() }
        fun kb(k: String): Long? = meminfo.firstOrNull { it.startsWith(k) }
            ?.filter { it.isDigit() }?.toLongOrNull()?.times(1024)

        val kullanilan = mi.totalMem - mi.availMem
        val ramYuzde = (kullanilan * 100 / mi.totalMem).toInt()

        kart("🧠", "RAM").also { k ->
            satir(k, "Toplam", gb(mi.totalMem))
            satir(k, "Kullanılan", "${gb(kullanilan)} (%$ramYuzde)")
            satir(k, "Boş", gb(mi.availMem))
            satir(k, "Düşük bellek eşiği", gb(mi.threshold))
            satir(k, "Düşük bellek modu", if (mi.lowMemory) "Evet ⚠️" else "Hayır")
            satir(k, "Uygulama heap sınırı", "${am.memoryClass} MB")
            satir(k, "Büyük heap sınırı", "${am.largeMemoryClass} MB")
            bar(k, ramYuzde, barRengi(ramYuzde))
        }

        val swapT = kb("SwapTotal")
        val swapF = kb("SwapFree")
        kart("🔁", "SWAP / ZRAM").also { k ->
            if (swapT != null && swapT > 0) {
                satir(k, "Toplam", gb(swapT))
                satir(k, "Boş", gb(swapF ?: 0))
            } else {
                satir(k, "Durum", "Yok / okunamadı")
            }
            satir(k, "Önbellek (Cached)", kb("Cached")?.let { gb(it) })
            satir(k, "Buffers", kb("Buffers")?.let { gb(it) })
        }

        val st = StatFs(Environment.getDataDirectory().path)
        val dKul = st.totalBytes - st.availableBytes
        val dYuzde = (dKul * 100 / st.totalBytes).toInt()
        kart("💾", "DAHİLİ DEPOLAMA").also { k ->
            satir(k, "Toplam", gb(st.totalBytes))
            satir(k, "Kullanılan", "${gb(dKul)} (%$dYuzde)")
            satir(k, "Boş", gb(st.availableBytes))
            bar(k, dYuzde, barRengi(dYuzde))
        }

        val sys = StatFs(Environment.getRootDirectory().path)
        val harici = getExternalFilesDirs(null).filterNotNull().size
        kart("🗂️", "DİĞER").also { k ->
            satir(k, "Sistem bölümü toplam", gb(sys.totalBytes))
            satir(k, "Sistem bölümü boş", gb(sys.availableBytes))
            satir(k, "Depolama alanı sayısı", "$harici" + if (harici > 1) " (SD kart olabilir)" else "")
        }
    }

    // ---------- SEKME: EKRAN ----------

    private fun sekmeEkran() {
        @Suppress("DEPRECATION")
        val d = windowManager.defaultDisplay
        val rm = DisplayMetrics()
        @Suppress("DEPRECATION")
        d.getRealMetrics(rm)
        val dm = resources.displayMetrics

        val wInc = rm.widthPixels / rm.xdpi
        val hInc = rm.heightPixels / rm.ydpi
        val inc = sqrt((wInc * wInc + hInc * hInc).toDouble())

        val hizlar = try {
            d.supportedModes.map { it.refreshRate.roundToInt() }.distinct().sorted()
                .joinToString(", ") { "$it Hz" }
        } catch (e: Exception) { null }

        val dpiGrup = when {
            dm.densityDpi <= 120 -> "ldpi"
            dm.densityDpi <= 160 -> "mdpi"
            dm.densityDpi <= 240 -> "hdpi"
            dm.densityDpi <= 320 -> "xhdpi"
            dm.densityDpi <= 480 -> "xxhdpi"
            else -> "xxxhdpi"
        }
        val cfg = resources.configuration

        kart("🖥️", "PANEL").also { k ->
            satir(k, "Çözünürlük", "${rm.widthPixels} x ${rm.heightPixels}")
            satir(k, "Boyut (yaklaşık)", String.format("%.1f inç", inc))
            satir(k, "Yenileme hızı (şu an)", "${d.refreshRate.roundToInt()} Hz")
            satir(k, "Desteklenen hızlar", hizlar)
            satir(k, "En-boy oranı", String.format("%.2f : 1", maxOf(rm.widthPixels, rm.heightPixels).toFloat() / minOf(rm.widthPixels, rm.heightPixels)))
        }
        kart("🔍", "YOĞUNLUK").also { k ->
            satir(k, "DPI", "${dm.densityDpi} ($dpiGrup)")
            satir(k, "Gerçek DPI (x / y)", String.format("%.0f / %.0f", rm.xdpi, rm.ydpi))
            satir(k, "Yoğunluk çarpanı", "${dm.density}")
            satir(k, "Yazı boyutu çarpanı", "${cfg.fontScale}")
            satir(k, "Kullanılabilir alan (dp)", "${cfg.screenWidthDp} x ${cfg.screenHeightDp}")
        }
        kart("🎨", "RENK").also { k ->
            satir(k, "Geniş renk gamı", evetHayir(cfg.isScreenWideColorGamut))
            satir(k, "HDR", evetHayir(cfg.isScreenHdr))
            satir(k, "Yönelim", if (cfg.orientation == 2) "Yatay" else "Dikey")
            satir(k, "Karanlık mod", if ((cfg.uiMode and 0x30) == 0x20) "Açık" else "Kapalı")
        }
    }

    // ---------- SEKME: PİL ----------

    private fun sekmePil() {
        val bat = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val yuzde = if (level >= 0) level * 100 / scale else 0
        val status = bat?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val health = bat?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val plug = bat?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val sicaklik = (bat?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val voltaj = bat?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val teknoloji = bat?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

        val durumTxt = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Şarj oluyor ⚡"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Deşarj oluyor"
            BatteryManager.BATTERY_STATUS_FULL -> "Dolu"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Şarj olmuyor"
            else -> "Bilinmiyor"
        }
        val saglikTxt = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "İyi"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Aşırı ısınmış"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Bozuk"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Aşırı voltaj"
            BatteryManager.BATTERY_HEALTH_COLD -> "Soğuk"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Arıza"
            else -> "Bilinmiyor"
        }
        val fisTxt = when (plug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Şarj adaptörü"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Kablosuz"
            else -> "Takılı değil"
        }

        val sayac = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val akim = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

        val tasarim: Int? = try {
            val c = Class.forName("com.android.internal.os.PowerProfile")
            val o = c.getConstructor(Context::class.java).newInstance(this)
            (c.getMethod("getBatteryCapacity").invoke(o) as Double).toInt()
        } catch (e: Exception) { null }

        val pilRenk = if (yuzde <= 20) Color.parseColor("#FF5C5C") else Color.parseColor("#4ADE80")

        kart("🔋", "DURUM").also { k ->
            satir(k, "Seviye", "%$yuzde")
            satir(k, "Durum", durumTxt)
            satir(k, "Sağlık", saglikTxt)
            satir(k, "Güç kaynağı", fisTxt)
            bar(k, yuzde, pilRenk)
        }
        kart("⚡", "ELEKTRİKSEL").also { k ->
            satir(k, "Sıcaklık", "$sicaklik °C")
            satir(k, "Voltaj", "$voltaj mV")
            satir(k, "Anlık akım", if (akim != 0 && akim != Int.MIN_VALUE) "${kotlin.math.abs(akim) / 1000} mA" else null)
            satir(k, "Kalan şarj", if (sayac > 0) "${sayac / 1000} mAh" else null)
            satir(k, "Teknoloji", teknoloji)
            satir(k, "Tasarım kapasitesi", tasarim?.let { if (it > 0) "$it mAh" else null })
        }
    }

    // ---------- SEKME: KAMERA ----------

    private fun sekmeKamera() {
        val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val idler = cm.cameraIdList
        if (idler.isEmpty()) {
            satir(kart("📷", "KAMERA"), "Durum", "Kamera bulunamadı")
            return
        }
        for (id in idler) {
            val c = cm.getCameraCharacteristics(id)
            val yon = when (c.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Ön"
                CameraCharacteristics.LENS_FACING_BACK -> "Arka"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Harici"
                else -> "?"
            }
            val px = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            val mp = px?.let { String.format("%.1f MP", it.width * it.height / 1000000.0) }
            val cozunurluk = px?.let { "${it.width} x ${it.height}" }
            val odak = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                ?.joinToString(", ") { String.format("%.1f mm", it) }
            val diyafram = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                ?.joinToString(", ") { String.format("f/%.1f", it) }
            val fiziksel = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                ?.let { String.format("%.2f x %.2f mm", it.width, it.height) }
            val flas = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val ois = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                ?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON } == true

            kart("📷", "$yon KAMERA (ID $id)").also { k ->
                satir(k, "Çözünürlük", "$mp ($cozunurluk)")
                satir(k, "Odak uzaklığı", odak)
                satir(k, "Diyafram", diyafram)
                satir(k, "Sensör boyutu", fiziksel)
                satir(k, "Flaş", evetHayir(flas))
                satir(k, "Optik sabitleme (OIS)", evetHayir(ois))
            }
        }
    }

    // ---------- SEKME: SENSÖRLER ----------

    private fun sekmeSensor() {
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val liste = sm.getSensorList(Sensor.TYPE_ALL)
        kart("📡", "TOPLAM").also { k ->
            satir(k, "Sensör sayısı", "${liste.size}")
        }
        val k = kart("📡", "SENSÖRLER")
        for (s in liste) {
            val tip = s.stringType.substringAfterLast('.').replace('_', ' ')
            satir(k, s.name, "$tip\n${s.vendor}")
        }
    }

    // ---------- SEKME: AĞ ----------

    private fun sekmeAg() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val cap = if (net != null) cm.getNetworkCapabilities(net) else null

        val tur = when {
            cap == null -> "Bağlantı yok"
            cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobil veri"
            cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            cap.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
            else -> "Diğer"
        }

        kart("🌐", "BAĞLANTI").also { k ->
            satir(k, "Tür", tur)
            satir(k, "İnternet erişimi", cap?.let { evetHayir(it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) })
            satir(k, "VPN", cap?.let { if (it.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) "Açık" else "Kapalı" })
            satir(k, "Ücretli (metered)", cap?.let { if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) "Hayır" else "Evet" })
            satir(k, "İndirme hızı (tahmini)", cap?.let { "${it.linkDownstreamBandwidthKbps / 1000} Mbps" })
            satir(k, "Yükleme hızı (tahmini)", cap?.let { "${it.linkUpstreamBandwidthKbps / 1000} Mbps" })
        }

        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        kart("📶", "OPERATÖR").also { k ->
            satir(k, "Şebeke", tm.networkOperatorName)
            satir(k, "SIM operatörü", tm.simOperatorName)
            satir(k, "SIM ülkesi", tm.simCountryIso?.uppercase())
            satir(k, "Şebeke ülkesi", tm.networkCountryIso?.uppercase())
            satir(k, "Dolaşım (roaming)", if (tm.isNetworkRoaming) "Evet" else "Hayır")
        }

        val ipler = try {
            Collections.list(NetworkInterface.getNetworkInterfaces()).flatMap { ni ->
                Collections.list(ni.inetAddresses)
                    .filter { !it.isLoopbackAddress }
                    .map { ni.name to (it.hostAddress ?: "").substringBefore('%') }
            }
        } catch (e: Exception) { emptyList() }

        val k = kart("🔗", "IP ADRESLERİ")
        if (ipler.isEmpty()) satir(k, "Durum", "Adres yok")
        for ((ad, ip) in ipler) satir(k, ad, ip)
    }

    // ---------- SEKME: ÖZELLİKLER ----------

    private fun sekmeOzellik() {
        val pm = packageManager
        val liste = listOf(
            "NFC" to PackageManager.FEATURE_NFC,
            "Bluetooth" to PackageManager.FEATURE_BLUETOOTH,
            "Bluetooth LE" to PackageManager.FEATURE_BLUETOOTH_LE,
            "Wi-Fi" to PackageManager.FEATURE_WIFI,
            "Wi-Fi Direct" to PackageManager.FEATURE_WIFI_DIRECT,
            "Wi-Fi Aware" to PackageManager.FEATURE_WIFI_AWARE,
            "Wi-Fi RTT" to PackageManager.FEATURE_WIFI_RTT,
            "GPS / Konum" to PackageManager.FEATURE_LOCATION_GPS,
            "Telefon" to PackageManager.FEATURE_TELEPHONY,
            "eSIM" to PackageManager.FEATURE_TELEPHONY_EUICC,
            "UWB" to "android.hardware.uwb",
            "Kızılötesi (IR)" to PackageManager.FEATURE_CONSUMER_IR,
            "USB Host (OTG)" to PackageManager.FEATURE_USB_HOST,
            "USB Accessory" to PackageManager.FEATURE_USB_ACCESSORY,
            "Parmak izi" to PackageManager.FEATURE_FINGERPRINT,
            "Yüz tanıma" to "android.hardware.biometrics.face",
            "StrongBox güvenlik çipi" to PackageManager.FEATURE_STRONGBOX_KEYSTORE,
            "Dokunmatik ekran" to PackageManager.FEATURE_TOUCHSCREEN,
            "Çoklu dokunma" to PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH,
            "Mikrofon" to PackageManager.FEATURE_MICROPHONE,
            "Düşük gecikmeli ses" to PackageManager.FEATURE_AUDIO_LOW_LATENCY,
            "Pro ses" to PackageManager.FEATURE_AUDIO_PRO,
            "Pusula" to PackageManager.FEATURE_SENSOR_COMPASS,
            "Jiroskop" to PackageManager.FEATURE_SENSOR_GYROSCOPE,
            "Barometre" to PackageManager.FEATURE_SENSOR_BAROMETER,
            "Yakınlık sensörü" to PackageManager.FEATURE_SENSOR_PROXIMITY,
            "Işık sensörü" to PackageManager.FEATURE_SENSOR_LIGHT,
            "Adım sayar" to PackageManager.FEATURE_SENSOR_STEP_COUNTER,
            "Kalp ritmi" to PackageManager.FEATURE_SENSOR_HEART_RATE
        )
        kart("✨", "GENEL").also { k ->
            satir(k, "Toplam sistem özelliği", "${pm.systemAvailableFeatures.size}")
        }
        val k = kart("✨", "DONANIM ÖZELLİKLERİ")
        for ((ad, f) in liste) satir(k, ad, evetHayir(pm.hasSystemFeature(f)))
    }
}
