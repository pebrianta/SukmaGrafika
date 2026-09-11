package com.sukmagrafika.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("config", MODE_PRIVATE) }
    private lateinit var content: LinearLayout

    private fun dp(v:Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s:String, size:Float=15f, bold:Boolean=false): TextView {
        val v = TextView(this)
        v.text = s
        v.textSize = size
        v.setTextColor(Color.rgb(25,35,55))
        v.setPadding(dp(8),dp(8),dp(8),dp(8))
        if (bold) v.setTypeface(null, 1)
        return v
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        dashboard()
    }

    private fun shell(title:String): LinearLayout {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(246,248,252))

        val head = TextView(this)
        head.text = "  $title"
        head.textSize = 21f
        head.gravity = Gravity.CENTER_VERTICAL
        head.setTextColor(Color.WHITE)
        head.setBackgroundColor(Color.rgb(21,101,192))
        root.addView(head, LinearLayout.LayoutParams(-1,dp(64)))

        val scroll = ScrollView(this)
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14),dp(14),dp(14),dp(90))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        val nav = LinearLayout(this)
        nav.setBackgroundColor(Color.WHITE)
        val names = listOf("Beranda","Pesanan","Laporan","Pengaturan")
        for (n in names) {
            val b = Button(this)
            b.text = n
            b.setOnClickListener {
                when(n) {
                    "Beranda" -> dashboard()
                    "Pesanan" -> orders()
                    "Laporan" -> report()
                    else -> settings()
                }
            }
            nav.addView(b, LinearLayout.LayoutParams(0,dp(58),1f))
        }
        root.addView(nav)
        return root
    }

    private fun card(label:String, value:String) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.WHITE)
        box.setPadding(dp(16),dp(10),dp(16),dp(10))
        box.addView(text(label,13f))
        box.addView(text(value,23f,true))
        val p = LinearLayout.LayoutParams(-1,dp(88))
        p.setMargins(0,0,0,dp(10))
        content.addView(box,p)
    }

    private fun dashboard() {
        setContentView(shell("SukmaGrafika"))
        content.addView(text("Dashboard Kasir",24f,true))
        card("OMZET HARI INI","Rp 0")
        card("PESANAN AKTIF","0")
        card("BELUM LUNAS","Rp 0")
        val b=Button(this); b.text="＋ PESANAN BARU"; b.setOnClickListener{newOrder()}
        content.addView(b)
    }

    private fun orders() {
        setContentView(shell("Pesanan"))
        content.addView(text("Riwayat Pesanan",23f,true))
        content.addView(text("Pesanan tersimpan di Google Sheets akan ditampilkan di sini."))
        val b=Button(this); b.text="＋ Buat Pesanan"; b.setOnClickListener{newOrder()}
        content.addView(b)
    }

    private fun report() {
        setContentView(shell("Laporan"))
        content.addView(text("Laporan Penjualan",23f,true))
        content.addView(text("Rekap omzet harian, mingguan, dan bulanan siap dikembangkan pada versi berikutnya."))
    }

    private fun settings() {
        setContentView(shell("Pengaturan"))
        content.addView(text("Google Sheets",23f,true))
        val e=EditText(this)
        e.hint="URL Google Apps Script Web App"
        e.setText(prefs.getString("url",""))
        content.addView(e)
        val b=Button(this)
        b.text="SIMPAN"
        b.setOnClickListener {
            prefs.edit().putString("url",e.text.toString().trim()).apply()
            Toast.makeText(this,"URL tersimpan",Toast.LENGTH_SHORT).show()
        }
        content.addView(b)
    }

    private fun newOrder() {
        setContentView(shell("Pesanan Baru"))
        content.addView(text("Input Pesanan",23f,true))

        val name=EditText(this); name.hint="Nama pelanggan"; content.addView(name)
        val wa=EditText(this); wa.hint="No. WhatsApp"; content.addView(wa)
        val product=EditText(this); product.hint="Produk / jenis cetak"; content.addView(product)
        val qty=EditText(this); qty.hint="Jumlah"; qty.inputType=2; content.addView(qty)
        val total=EditText(this); total.hint="Total harga"; total.inputType=2; content.addView(total)
        val dp=EditText(this); dp.hint="DP"; dp.inputType=2; content.addView(dp)
        val status=EditText(this); status.hint="Status"; status.setText("Baru"); content.addView(status)

        val b=Button(this)
        b.text="SIMPAN PESANAN"
        b.setOnClickListener {
            val url=prefs.getString("url","") ?: ""
            if(url.isBlank()) {
                Toast.makeText(this,"Masukkan URL Google Apps Script di Pengaturan",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val payload = """{"action":"createOrder","customer":"${esc(name.text.toString())}","whatsapp":"${esc(wa.text.toString())}","product":"${esc(product.text.toString())}","qty":"${qty.text}","total":"${total.text}","dp":"${dp.text}","status":"${esc(status.text.toString())}"}"""
            thread {
                try {
                    val c=URL(url).openConnection() as HttpURLConnection
                    c.requestMethod="POST"
                    c.doOutput=true
                    c.setRequestProperty("Content-Type","application/json")
                    c.outputStream.use{it.write(payload.toByteArray())}
                    val code=c.responseCode
                    runOnUiThread {
                        Toast.makeText(this,"Pesanan tersimpan (HTTP $code)",Toast.LENGTH_SHORT).show()
                        orders()
                    }
                } catch(ex:Exception) {
                    runOnUiThread { Toast.makeText(this,"Gagal: ${ex.message}",Toast.LENGTH_LONG).show() }
                }
            }
        }
        content.addView(b)
    }

    private fun esc(s:String) = s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ")
}
