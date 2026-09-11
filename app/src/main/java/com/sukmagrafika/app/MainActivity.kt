package com.sukmagrafika.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("config", MODE_PRIVATE) }
    private lateinit var content: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float = 15f, bold: Boolean = false): TextView {
        val v = TextView(this)
        v.text = s
        v.textSize = size
        v.setTextColor(Color.rgb(25, 35, 55))
        v.setPadding(dp(8), dp(8), dp(8), dp(8))
        if (bold) v.setTypeface(null, 1)
        return v
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        dashboard()
    }

    private fun shell(title: String): LinearLayout {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(246, 248, 252))

        val head = TextView(this)
        head.text = "  $title"
        head.textSize = 21f
        head.gravity = Gravity.CENTER_VERTICAL
        head.setTextColor(Color.WHITE)
        head.setBackgroundColor(Color.rgb(21, 101, 192))
        root.addView(head, LinearLayout.LayoutParams(-1, dp(64)))

        val scroll = ScrollView(this)
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14), dp(14), dp(14), dp(90))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this)
        nav.setBackgroundColor(Color.WHITE)
        val names = listOf("Beranda", "Pesanan", "Laporan", "Pengaturan")
        for (n in names) {
            val b = Button(this)
            b.text = n
            b.setOnClickListener {
                when (n) {
                    "Beranda" -> dashboard()
                    "Pesanan" -> orders()
                    "Laporan" -> report()
                    else -> settings()
                }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, dp(58), 1f))
        }
        root.addView(nav)
        return root
    }

    private fun card(label: String, value: String) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.WHITE)
        box.setPadding(dp(16), dp(10), dp(16), dp(10))
        box.addView(text(label, 13f))
        box.addView(text(value, 23f, true))
        val p = LinearLayout.LayoutParams(-1, dp(88))
        p.setMargins(0, 0, 0, dp(10))
        content.addView(box, p)
    }

    private fun dashboard() {
        setContentView(shell("SukmaGrafika"))
        content.addView(text("Dashboard Kasir", 24f, true))
        card("OMZET HARI INI", "Memuat...")
        card("PESANAN AKTIF", "Memuat...")
        card("BELUM LUNAS", "Memuat...")
        val b = Button(this)
        b.text = "＋ PESANAN BARU"
        b.setOnClickListener { newOrder() }
        content.addView(b)

        fetchOrders { rows ->
            runOnUiThread {
                if (rows == null) {
                    Toast.makeText(this, "Gagal mengambil data Google Sheets", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val s = calculate(rows)
                // Rebuild only dashboard values.
                dashboardWithStats(s)
            }
        }
    }

    private fun dashboardWithStats(s: Stats) {
        setContentView(shell("SukmaGrafika"))
        content.addView(text("Dashboard Kasir", 24f, true))
        card("OMZET HARI INI", rupiah(s.todayRevenue))
        card("PESANAN AKTIF", s.activeOrders.toString())
        card("BELUM LUNAS", rupiah(s.unpaid))
        val b = Button(this)
        b.text = "＋ PESANAN BARU"
        b.setOnClickListener { newOrder() }
        content.addView(b)
    }

    private fun orders() {
        setContentView(shell("Pesanan"))
        content.addView(text("Riwayat Pesanan", 23f, true))
        val loading = text("Memuat data...")
        content.addView(loading)

        fetchOrders { rows ->
            runOnUiThread {
                content.removeView(loading)
                if (rows == null) {
                    content.addView(text("Gagal mengambil data. Periksa URL Apps Script di Pengaturan."))
                    return@runOnUiThread
                }
                if (rows.isEmpty()) {
                    content.addView(text("Belum ada pesanan."))
                } else {
                    for (r in rows.takeLast(100).reversed()) {
                        val box = LinearLayout(this)
                        box.orientation = LinearLayout.VERTICAL
                        box.setBackgroundColor(Color.WHITE)
                        box.setPadding(dp(12), dp(8), dp(12), dp(8))
                        val id = r.getString(0)
                        val customer = r.getString(2)
                        val product = r.getString(4)
                        val qty = r.getString(5)
                        val total = r.getString(6)
                        val dp = r.getString(7)
                        val status = r.getString(9)
                        box.addView(text("$id • $customer", 16f, true))
                        box.addView(text("$product • Qty $qty\nTotal: ${rupiah(total.toDoubleOrNull() ?: 0.0)} • DP: ${rupiah(dp.toDoubleOrNull() ?: 0.0)}\nStatus: $status"))
                        val p = LinearLayout.LayoutParams(-1, -2)
                        p.setMargins(0, 0, 0, dp(8))
                        content.addView(box, p)
                    }
                }
                val b = Button(this)
                b.text = "＋ Buat Pesanan"
                b.setOnClickListener { newOrder() }
                content.addView(b)
            }
        }
    }

    private fun report() {
        setContentView(shell("Laporan"))
        content.addView(text("Laporan Penjualan", 23f, true))
        val loading = text("Menghitung laporan...")
        content.addView(loading)

        fetchOrders { rows ->
            runOnUiThread {
                content.removeView(loading)
                if (rows == null) {
                    content.addView(text("Gagal mengambil data. Periksa koneksi dan URL Apps Script."))
                    return@runOnUiThread
                }
                val s = calculate(rows)
                card("OMZET HARI INI", rupiah(s.todayRevenue))
                card("OMZET MINGGU INI", rupiah(s.weekRevenue))
                card("OMZET BULAN INI", rupiah(s.monthRevenue))
                card("TOTAL OMZET", rupiah(s.totalRevenue))
                card("TOTAL PESANAN", s.totalOrders.toString())
                card("BELUM LUNAS", rupiah(s.unpaid))
                content.addView(text("Status Pesanan", 19f, true))
                content.addView(text(
                    "Baru: ${s.baru}\nProses: ${s.proses}\nSelesai: ${s.selesai}\nLunas: ${s.lunas}"
                ))
            }
        }
    }

    private fun settings() {
        setContentView(shell("Pengaturan"))
        content.addView(text("Google Sheets", 23f, true))
        val e = EditText(this)
        e.hint = "URL Google Apps Script Web App"
        e.setText(prefs.getString("url", ""))
        content.addView(e)
        val b = Button(this)
        b.text = "SIMPAN"
        b.setOnClickListener {
            prefs.edit().putString("url", e.text.toString().trim()).apply()
            Toast.makeText(this, "URL tersimpan", Toast.LENGTH_SHORT).show()
        }
        content.addView(b)
        content.addView(text("Gunakan URL Web App yang berakhiran /exec."))
    }

    private fun newOrder() {
        setContentView(shell("Pesanan Baru"))
        content.addView(text("Input Pesanan", 23f, true))

        val name = EditText(this); name.hint = "Nama pelanggan"; content.addView(name)
        val wa = EditText(this); wa.hint = "No. WhatsApp"; content.addView(wa)
        val product = EditText(this); product.hint = "Produk / jenis cetak"; content.addView(product)
        val qty = EditText(this); qty.hint = "Jumlah"; qty.inputType = 2; content.addView(qty)
        val total = EditText(this); total.hint = "Total harga"; total.inputType = 2; content.addView(total)
        val dp = EditText(this); dp.hint = "DP"; dp.inputType = 2; content.addView(dp)
        val status = EditText(this); status.hint = "Status"; status.setText("Baru"); content.addView(status)

        val b = Button(this)
        b.text = "SIMPAN PESANAN"
        b.setOnClickListener {
            val url = prefs.getString("url", "") ?: ""
            if (url.isBlank()) {
                Toast.makeText(this, "Masukkan URL Google Apps Script di Pengaturan", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val payload = """{"action":"createOrder","customer":"${esc(name.text.toString())}","whatsapp":"${esc(wa.text.toString())}","product":"${esc(product.text.toString())}","qty":"${qty.text}","total":"${total.text}","dp":"${dp.text}","status":"${esc(status.text.toString())}"}"""
            thread {
                try {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.requestMethod = "POST"
                    c.doOutput = true
                    c.setRequestProperty("Content-Type", "application/json")
                    c.outputStream.use { it.write(payload.toByteArray()) }
                    val code = c.responseCode
                    runOnUiThread {
                        Toast.makeText(this, "Pesanan tersimpan (HTTP $code)", Toast.LENGTH_SHORT).show()
                        orders()
                    }
                } catch (ex: Exception) {
                    runOnUiThread { Toast.makeText(this, "Gagal: ${ex.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
        content.addView(b)
    }

    private fun fetchOrders(callback: (JSONArray?) -> Unit) {
        val url = prefs.getString("url", "") ?: ""
        if (url.isBlank()) {
            callback(null)
            return
        }
        thread {
            try {
                val full = if (url.contains("?")) "$url&action=orders" else "$url?action=orders"
                val c = URL(full).openConnection() as HttpURLConnection
                c.requestMethod = "GET"
                c.connectTimeout = 15000
                c.readTimeout = 20000
                val body = c.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(body)
                if (!obj.optBoolean("ok", false)) {
                    callback(null)
                    return@thread
                }
                val raw = obj.optJSONArray("data") ?: JSONArray()
                val rows = JSONArray()
                for (i in 1 until raw.length()) {
                    val r = raw.optJSONArray(i) ?: continue
                    if (r.length() >= 10) rows.put(r)
                }
                callback(rows)
            } catch (ex: Exception) {
                callback(null)
            }
        }
    }

    data class Stats(
        val todayRevenue: Double,
        val weekRevenue: Double,
        val monthRevenue: Double,
        val totalRevenue: Double,
        val unpaid: Double,
        val totalOrders: Int,
        val activeOrders: Int,
        val baru: Int,
        val proses: Int,
        val selesai: Int,
        val lunas: Int
    )

    private fun calculate(rows: JSONArray): Stats {
        val cal = Calendar.getInstance()
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)

        var today = 0.0
        var week = 0.0
        var month = 0.0
        var total = 0.0
        var unpaid = 0.0
        var active = 0
        var baru = 0
        var proses = 0
        var selesai = 0
        var lunas = 0

        for (i in 0 until rows.length()) {
            val r = rows.optJSONArray(i) ?: continue
            val dateText = r.optString(1)
            val revenue = r.optString(6).toDoubleOrNull() ?: 0.0
            val sisa = r.optString(8).toDoubleOrNull() ?: 0.0
            val status = r.optString(9).trim().lowercase(Locale.getDefault())

            total += revenue
            unpaid += maxOf(0.0, sisa)

            if (dateText.startsWith(todayKey)) today += revenue
            if (dateText.startsWith(monthKey)) month += revenue

            val date = parseDate(dateText)
            if (date != null) {
                val dcal = Calendar.getInstance()
                dcal.time = date
                if (dcal.get(Calendar.WEEK_OF_YEAR) == cal.get(Calendar.WEEK_OF_YEAR) &&
                    dcal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)) {
                    week += revenue
                }
            }

            when {
                status.contains("lunas") -> lunas++
                status.contains("selesai") -> selesai++
                status.contains("proses") -> { proses++; active++ }
                status.contains("baru") -> { baru++; active++ }
                status.isNotBlank() -> active++
            }
        }

        return Stats(today, week, month, total, unpaid, rows.length(), active, baru, proses, selesai, lunas)
    }

    private fun parseDate(s: String): Date? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(s)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun rupiah(value: Double): String =
        "Rp " + String.format(Locale.US, "%,.0f", value).replace(",", ".")

    private fun esc(s: String) =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")

    private fun String.toDoubleOrNull(): Double? = try { toDouble() } catch (_: Exception) { null }
}
