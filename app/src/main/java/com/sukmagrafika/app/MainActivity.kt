package com.sukmagrafika.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
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

    private val blue = Color.rgb(21, 101, 192)
    private val bg = Color.rgb(246, 248, 252)
    private val dark = Color.rgb(25, 35, 55)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float = 15f, bold: Boolean = false): TextView {
        val v = TextView(this)
        v.text = s
        v.textSize = size
        v.setTextColor(dark)
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
        root.setBackgroundColor(bg)

        val head = TextView(this)
        head.text = "  $title"
        head.textSize = 21f
        head.gravity = Gravity.CENTER_VERTICAL
        head.setTextColor(Color.WHITE)
        head.setBackgroundColor(blue)
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
                    Toast.makeText(
                        this,
                        "Gagal mengambil data Google Sheets",
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }

                val s = calculate(rows)
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
                    content.addView(
                        text("Gagal mengambil data. Periksa URL Apps Script di Pengaturan.")
                    )
                    return@runOnUiThread
                }

                if (rows.length() == 0) {
                    content.addView(text("Belum ada pesanan."))
                } else {
                    val start = maxOf(0, rows.length() - 100)

                    for (i in rows.length() - 1 downTo start) {
                        val r = rows.getJSONArray(i)
                        addOrderCard(r)
                    }
                }

                val b = Button(this)
                b.text = "＋ Buat Pesanan"
                b.setOnClickListener { newOrder() }
                content.addView(b)
            }
        }
    }

    private fun addOrderCard(r: JSONArray) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.WHITE)
        box.setPadding(dp(12), dp(10), dp(12), dp(10))

        val id = r.optString(0)
        val customer = r.optString(2)
        val whatsapp = r.optString(3)
        val product = r.optString(4)
        val qty = r.optString(5)
        val total = r.optString(6).toDoubleOrNullSafe()
        val dpValue = r.optString(7).toDoubleOrNullSafe()
        val sisa = r.optString(8).toDoubleOrNullSafe()
        val status = r.optString(9)

        box.addView(text("$id • $customer", 16f, true))
        box.addView(
            text(
                "$product • Qty $qty\n" +
                    "Total: ${rupiah(total)} • DP: ${rupiah(dpValue)}\n" +
                    "Sisa: ${rupiah(sisa)}\n" +
                    "WA: $whatsapp\n" +
                    "Status: $status"
            )
        )

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL

        val edit = Button(this)
        edit.text = "EDIT"
        edit.setOnClickListener { editOrder(r) }

        val delete = Button(this)
        delete.text = "HAPUS"
        delete.setOnClickListener { confirmDelete(id, customer) }

        actions.addView(edit, LinearLayout.LayoutParams(0, dp(52), 1f))
        actions.addView(delete, LinearLayout.LayoutParams(0, dp(52), 1f))
        box.addView(actions)

        val p = LinearLayout.LayoutParams(-1, -2)
        p.setMargins(0, 0, 0, dp(10))
        content.addView(box, p)
    }

    private fun editOrder(r: JSONArray) {
        setContentView(shell("Edit Pesanan"))
        content.addView(text("Edit Pesanan", 23f, true))

        val id = r.optString(0)

        val name = EditText(this)
        name.hint = "Nama pelanggan"
        name.setText(r.optString(2))
        content.addView(name)

        val wa = EditText(this)
        wa.hint = "No. WhatsApp"
        wa.inputType = InputType.TYPE_CLASS_PHONE
        wa.setText(r.optString(3))
        content.addView(wa)

        val product = EditText(this)
        product.hint = "Produk / jenis cetak"
        product.setText(r.optString(4))
        content.addView(product)

        val qty = EditText(this)
        qty.hint = "Jumlah"
        qty.inputType = InputType.TYPE_CLASS_NUMBER
        qty.setText(r.optString(5))
        content.addView(qty)

        val total = EditText(this)
        total.hint = "Total harga"
        total.inputType = InputType.TYPE_CLASS_NUMBER
        total.setText(r.optString(6))
        content.addView(total)

        val dp = EditText(this)
        dp.hint = "DP"
        dp.inputType = InputType.TYPE_CLASS_NUMBER
        dp.setText(r.optString(7))
        content.addView(dp)

        content.addView(text("Status", 14f, true))

        val status = Spinner(this)
        val statuses = arrayOf("Baru", "Proses", "Selesai", "Lunas")
        status.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )

        val currentStatus = r.optString(9)
        val selected = statuses.indexOfFirst {
            it.equals(currentStatus, ignoreCase = true)
        }
        status.setSelection(if (selected >= 0) selected else 0)
        content.addView(status)

        val save = Button(this)
        save.text = "SIMPAN PERUBAHAN"
        save.setOnClickListener {
            val url = prefs.getString("url", "") ?: ""

            if (url.isBlank()) {
                Toast.makeText(
                    this,
                    "Masukkan URL Google Apps Script di Pengaturan",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (name.text.toString().trim().isBlank()) {
                name.error = "Nama pelanggan wajib diisi"
                return@setOnClickListener
            }

            val payload = JSONObject()
                .put("action", "updateOrder")
                .put("id", id)
                .put("customer", name.text.toString().trim())
                .put("whatsapp", wa.text.toString().trim())
                .put("product", product.text.toString().trim())
                .put("qty", qty.text.toString().trim().ifBlank { "0" })
                .put("total", total.text.toString().trim().ifBlank { "0" })
                .put("dp", dp.text.toString().trim().ifBlank { "0" })
                .put("status", status.selectedItem.toString())

            save.isEnabled = false

            thread {
                val result = postJson(url, payload)

                runOnUiThread {
                    save.isEnabled = true

                    if (result.first) {
                        Toast.makeText(
                            this,
                            "Pesanan berhasil diperbarui",
                            Toast.LENGTH_SHORT
                        ).show()
                        orders()
                    } else {
                        Toast.makeText(
                            this,
                            "Gagal memperbarui: ${result.second}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        content.addView(save)

        val back = Button(this)
        back.text = "KEMBALI"
        back.setOnClickListener { orders() }
        content.addView(back)
    }

    private fun confirmDelete(id: String, customer: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus pesanan?")
            .setMessage("Pesanan $id atas nama $customer akan dihapus dari Google Sheets.")
            .setNegativeButton("BATAL", null)
            .setPositiveButton("HAPUS") { _, _ ->
                deleteOrder(id)
            }
            .show()
    }

    private fun deleteOrder(id: String) {
        val url = prefs.getString("url", "") ?: ""

        if (url.isBlank()) {
            Toast.makeText(
                this,
                "Masukkan URL Google Apps Script di Pengaturan",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val payload = JSONObject()
            .put("action", "deleteOrder")
            .put("id", id)

        thread {
            val result = postJson(url, payload)

            runOnUiThread {
                if (result.first) {
                    Toast.makeText(
                        this,
                        "Pesanan berhasil dihapus",
                        Toast.LENGTH_SHORT
                    ).show()
                    orders()
                } else {
                    Toast.makeText(
                        this,
                        "Gagal menghapus: ${result.second}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun report() {
        setContentView(shell("Laporan"))
        content.addView(text("Laporan Penjualan", 23f, true))

        val periods = arrayOf(
            "Hari Ini",
            "Kemarin",
            "7 Hari Terakhir",
            "Minggu Ini",
            "Bulan Ini",
            "Bulan Lalu",
            "Semua",
            "Custom"
        )

        content.addView(text("Periode laporan", 14f, true))

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            periods
        )
        content.addView(spinner)

        val customBox = LinearLayout(this)
        customBox.orientation = LinearLayout.HORIZONTAL
        customBox.visibility = View.GONE

        val startButton = Button(this)
        val endButton = Button(this)

        customBox.addView(startButton, LinearLayout.LayoutParams(0, dp(58), 1f))
        customBox.addView(endButton, LinearLayout.LayoutParams(0, dp(58), 1f))
        content.addView(customBox)

        val resultBox = LinearLayout(this)
        resultBox.orientation = LinearLayout.VERTICAL
        content.addView(resultBox)

        var customStart = startOfDay(Calendar.getInstance().time)
        var customEnd = startOfDay(Calendar.getInstance().time)

        fun updateDateButtons() {
            startButton.text = formatDate(customStart)
            endButton.text = formatDate(customEnd)
        }

        fun render(rows: JSONArray?) {
            if (rows == null) {
                resultBox.removeAllViews()
                resultBox.addView(
                    text("Gagal mengambil data. Periksa koneksi dan URL Apps Script.")
                )
                return
            }

            val period = periodFor(
                spinner.selectedItem.toString(),
                customStart,
                customEnd
            )

            val s = calculatePeriod(rows, period.first, period.second)

            resultBox.removeAllViews()
            resultBox.addView(text(period.third, 16f, true))
            resultBox.addView(space(4))

            addCardTo(resultBox, "OMZET", rupiah(s.revenue))
            addCardTo(resultBox, "TOTAL PESANAN", s.orders.toString())
            addCardTo(resultBox, "TOTAL DP", rupiah(s.dp))
            addCardTo(resultBox, "SISA TAGIHAN", rupiah(s.unpaid))

            resultBox.addView(text("Status Pesanan", 19f, true))
            resultBox.addView(
                text(
                    "Baru: ${s.baru}\n" +
                        "Proses: ${s.proses}\n" +
                        "Selesai: ${s.selesai}\n" +
                        "Lunas: ${s.lunas}"
                )
            )
        }

        startButton.setOnClickListener {
            val c = Calendar.getInstance()
            c.time = customStart

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selected = Calendar.getInstance()
                    selected.set(year, month, day, 0, 0, 0)
                    selected.set(Calendar.MILLISECOND, 0)
                    customStart = selected.time

                    if (customStart.after(customEnd)) {
                        customEnd = customStart
                    }

                    updateDateButtons()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        endButton.setOnClickListener {
            val c = Calendar.getInstance()
            c.time = customEnd

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selected = Calendar.getInstance()
                    selected.set(year, month, day, 0, 0, 0)
                    selected.set(Calendar.MILLISECOND, 0)
                    customEnd = selected.time

                    if (customEnd.before(customStart)) {
                        customStart = customEnd
                    }

                    updateDateButtons()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        updateDateButtons()

        val loading = text("Menghitung laporan...")
        resultBox.addView(loading)

        fetchOrders { rows ->
            runOnUiThread {
                resultBox.removeView(loading)
                render(rows)

                spinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {}

                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            customBox.visibility =
                                if (periods[position] == "Custom") View.VISIBLE else View.GONE

                            render(rows)
                        }
                    }
            }
        }

        spinner.setSelection(0)
    }

    private fun addCardTo(parent: LinearLayout, label: String, value: String) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.WHITE)
        box.setPadding(dp(16), dp(10), dp(16), dp(10))
        box.addView(text(label, 13f))
        box.addView(text(value, 23f, true))

        val p = LinearLayout.LayoutParams(-1, dp(88))
        p.setMargins(0, 0, 0, dp(10))
        parent.addView(box, p)
    }

    private fun space(height: Int): View {
        return Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(height))
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
            val url = e.text.toString().trim()

            if (url.isBlank()) {
                Toast.makeText(this, "URL belum diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString("url", url).apply()

            Toast.makeText(
                this,
                "URL tersimpan",
                Toast.LENGTH_SHORT
            ).show()
        }
        content.addView(b)

        content.addView(
            text(
                "Gunakan URL Web App yang berakhiran /exec.\n" +
                    "Jangan gunakan URL /dev untuk aplikasi kasir."
            )
        )

        val test = Button(this)
        test.text = "TES KONEKSI"
        test.setOnClickListener {
            fetchOrders { rows ->
                runOnUiThread {
                    if (rows != null) {
                        Toast.makeText(
                            this,
                            "Koneksi Google Sheets berhasil",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Koneksi gagal. Periksa URL dan deployment Apps Script.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        content.addView(test)
    }

    private fun newOrder() {
        setContentView(shell("Pesanan Baru"))
        content.addView(text("Input Pesanan", 23f, true))

        val name = EditText(this)
        name.hint = "Nama pelanggan"
        content.addView(name)

        val wa = EditText(this)
        wa.hint = "No. WhatsApp"
        wa.inputType = InputType.TYPE_CLASS_PHONE
        content.addView(wa)

        val product = EditText(this)
        product.hint = "Produk / jenis cetak"
        content.addView(product)

        val qty = EditText(this)
        qty.hint = "Jumlah"
        qty.inputType = InputType.TYPE_CLASS_NUMBER
        content.addView(qty)

        val total = EditText(this)
        total.hint = "Total harga"
        total.inputType = InputType.TYPE_CLASS_NUMBER
        content.addView(total)

        val dpValue = EditText(this)
        dpValue.hint = "DP"
        dpValue.inputType = InputType.TYPE_CLASS_NUMBER
        content.addView(dpValue)

        content.addView(text("Status", 14f, true))

        val status = Spinner(this)
        val statuses = arrayOf("Baru", "Proses", "Selesai", "Lunas")
        status.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
        content.addView(status)

        val b = Button(this)
        b.text = "SIMPAN PESANAN"

        b.setOnClickListener {
            val url = prefs.getString("url", "") ?: ""

            if (url.isBlank()) {
                Toast.makeText(
                    this,
                    "Masukkan URL Google Apps Script di Pengaturan",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (name.text.toString().trim().isBlank()) {
                name.error = "Nama pelanggan wajib diisi"
                return@setOnClickListener
            }

            val payload = JSONObject()
                .put("action", "createOrder")
                .put("customer", name.text.toString().trim())
                .put("whatsapp", wa.text.toString().trim())
                .put("product", product.text.toString().trim())
                .put("qty", qty.text.toString().trim().ifBlank { "0" })
                .put("total", total.text.toString().trim().ifBlank { "0" })
                .put("dp", dpValue.text.toString().trim().ifBlank { "0" })
                .put("status", status.selectedItem.toString())

            b.isEnabled = false

            thread {
                val result = postJson(url, payload)

                runOnUiThread {
                    b.isEnabled = true

                    if (result.first) {
                        Toast.makeText(
                            this,
                            "Pesanan berhasil disimpan",
                            Toast.LENGTH_SHORT
                        ).show()
                        orders()
                    } else {
                        Toast.makeText(
                            this,
                            "Gagal menyimpan: ${result.second}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
                val full =
                    if (url.contains("?")) "$url&action=orders"
                    else "$url?action=orders"

                val c = URL(full).openConnection() as HttpURLConnection
                c.requestMethod = "GET"
                c.connectTimeout = 15000
                c.readTimeout = 20000

                val body =
                    c.inputStream.bufferedReader().use { it.readText() }

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
            } catch (_: Exception) {
                callback(null)
            }
        }
    }

    private fun postJson(url: String, payload: JSONObject): Pair<Boolean, String> {
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.doOutput = true
            c.connectTimeout = 15000
            c.readTimeout = 20000
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            c.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = c.responseCode
            val stream =
                if (code in 200..399) c.inputStream else c.errorStream

            val body =
                stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                return Pair(false, "HTTP $code")
            }

            val obj = try {
                JSONObject(body)
            } catch (_: Exception) {
                JSONObject()
            }

            if (obj.optBoolean("ok", false)) {
                Pair(true, obj.optString("message", "Berhasil"))
            } else {
                Pair(false, obj.optString("error", "Server menolak permintaan"))
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Koneksi gagal")
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
        val today = startOfDay(Date())
        val cal = Calendar.getInstance()
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)

        var todayRevenue = 0.0
        var weekRevenue = 0.0
        var monthRevenue = 0.0
        var totalRevenue = 0.0
        var unpaid = 0.0
        var active = 0
        var baru = 0
        var proses = 0
        var selesai = 0
        var lunas = 0

        val weekStart = startOfWeek(Date())

        for (i in 0 until rows.length()) {
            val r = rows.optJSONArray(i) ?: continue
            val dateText = r.optString(1)
            val revenue = r.optString(6).toDoubleOrNullSafe()
            val sisa = r.optString(8).toDoubleOrNullSafe()
            val status = r.optString(9).trim().lowercase(Locale.getDefault())

            totalRevenue += revenue
            unpaid += maxOf(0.0, sisa)

            if (dateText.startsWith(todayKey)) {
                todayRevenue += revenue
            }

            if (dateText.startsWith(monthKey)) {
                monthRevenue += revenue
            }

            val date = parseDate(dateText)

            if (date != null && !date.before(weekStart)) {
                weekRevenue += revenue
            }

            when {
                status.contains("lunas") -> lunas++
                status.contains("selesai") -> selesai++
                status.contains("proses") -> {
                    proses++
                    active++
                }
                status.contains("baru") -> {
                    baru++
                    active++
                }
                status.isNotBlank() -> active++
            }
        }

        return Stats(
            todayRevenue,
            weekRevenue,
            monthRevenue,
            totalRevenue,
            unpaid,
            rows.length(),
            active,
            baru,
            proses,
            selesai,
            lunas
        )
    }

    data class PeriodStats(
        val revenue: Double,
        val orders: Int,
        val dp: Double,
        val unpaid: Double,
        val baru: Int,
        val proses: Int,
        val selesai: Int,
        val lunas: Int
    )

    private fun calculatePeriod(
        rows: JSONArray,
        start: Date?,
        end: Date?
    ): PeriodStats {
        var revenue = 0.0
        var orders = 0
        var dp = 0.0
        var unpaid = 0.0
        var baru = 0
        var proses = 0
        var selesai = 0
        var lunas = 0

        for (i in 0 until rows.length()) {
            val r = rows.optJSONArray(i) ?: continue
            val date = parseDate(r.optString(1)) ?: continue

            val day = startOfDay(date)

            if (start != null && day.before(startOfDay(start))) continue
            if (end != null && day.after(startOfDay(end))) continue

            val total = r.optString(6).toDoubleOrNullSafe()
            val dpValue = r.optString(7).toDoubleOrNullSafe()
            val sisa = r.optString(8).toDoubleOrNullSafe()
            val status = r.optString(9).trim().lowercase(Locale.getDefault())

            revenue += total
            dp += dpValue
            unpaid += maxOf(0.0, sisa)
            orders++

            when {
                status.contains("lunas") -> lunas++
                status.contains("selesai") -> selesai++
                status.contains("proses") -> proses++
                status.contains("baru") -> baru++
            }
        }

        return PeriodStats(
            revenue,
            orders,
            dp,
            unpaid,
            baru,
            proses,
            selesai,
            lunas
        )
    }

    private fun periodFor(
        name: String,
        customStart: Date,
        customEnd: Date
    ): Triple<Date?, Date?, String> {
        val today = startOfDay(Date())
        val c = Calendar.getInstance()
        c.time = today

        return when (name) {
            "Hari Ini" -> Triple(today, today, "Hari Ini")

            "Kemarin" -> {
                c.add(Calendar.DAY_OF_YEAR, -1)
                val d = c.time
                Triple(d, d, "Kemarin • ${formatDate(d)}")
            }

            "7 Hari Terakhir" -> {
                c.add(Calendar.DAY_OF_YEAR, -6)
                Triple(c.time, today, "7 Hari Terakhir")
            }

            "Minggu Ini" -> {
                val start = startOfWeek(today)
                Triple(start, today, "Minggu Ini")
            }

            "Bulan Ini" -> {
                c.set(Calendar.DAY_OF_MONTH, 1)
                Triple(c.time, today, "Bulan Ini")
            }

            "Bulan Lalu" -> {
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.add(Calendar.MONTH, -1)
                val start = c.time
                c.add(Calendar.MONTH, 1)
                c.add(Calendar.DAY_OF_YEAR, -1)
                Triple(start, c.time, "Bulan Lalu")
            }

            "Semua" -> Triple(null, null, "Semua Pesanan")

            else -> Triple(
                startOfDay(customStart),
                startOfDay(customEnd),
                "Custom: ${formatDate(customStart)} - ${formatDate(customEnd)}"
            )
        }
    }

    private fun startOfDay(date: Date): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    private fun startOfWeek(date: Date): Date {
        val c = Calendar.getInstance()
        c.time = startOfDay(date)
        c.firstDayOfWeek = Calendar.MONDAY
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return c.time
    }

    private fun parseDate(s: String): Date? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss"
        )

        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US)
                if (f.contains("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(s)
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date)
    }

    private fun rupiah(value: Double): String {
        return "Rp " +
            String.format(Locale.US, "%,.0f", value)
                .replace(",", ".")
    }

    private fun String.toDoubleOrNullSafe(): Double {
        return try {
            replace(",", "").trim().toDouble()
        } catch (_: Exception) {
            0.0
        }
    }
}
