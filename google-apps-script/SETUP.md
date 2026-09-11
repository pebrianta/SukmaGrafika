# Setup Google Sheets

1. Buat Google Spreadsheet baru: `Database SukmaGrafika`.
2. Buka Extensions -> Apps Script.
3. Masukkan isi `Code.gs`.
4. Jalankan `setup()` sekali dan izinkan akses.
5. Deploy -> New deployment -> Web app.
6. Execute as: Me.
7. Pilih akses yang sesuai (untuk penggunaan pribadi 1 admin, jangan membagikan URL).
8. Salin URL Web App.
9. Di aplikasi SukmaGrafika -> Pengaturan -> tempel URL -> Simpan.

Sheet `Orders` dibuat otomatis dengan kolom:
ID | Tanggal | Pelanggan | WhatsApp | Produk | Qty | Total | DP | Sisa | Status
