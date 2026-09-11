# Build APK SukmaGrafika langsung dari HP

## 1. Buat repository GitHub
- Buka GitHub di browser HP.
- Buat repository baru, misalnya `SukmaGrafika`.
- Pilih Public atau Private sesuai kebutuhan.

## 2. Upload proyek
Ekstrak `SukmaGrafika_Proyek_Lengkap.zip` di HP jika diperlukan, lalu upload seluruh isi folder `SukmaGrafika` ke repository.
PENTING: folder `.github/workflows/build-apk.yml` harus ikut ter-upload.

## 3. Jalankan build
- Buka repository.
- Pilih tab `Actions`.
- Pilih workflow `Build SukmaGrafika APK`.
- Tekan `Run workflow`.
- Tunggu sampai job berstatus hijau.

## 4. Download APK
- Buka hasil workflow yang berhasil.
- Scroll ke bagian `Artifacts`.
- Download `SukmaGrafika-APK`.
- Ekstrak ZIP artifact.
- File di dalamnya adalah `app-debug.apk`.
- Install APK di HP.

## Jika build gagal
Buka workflow yang gagal lalu lihat langkah yang berwarna merah. Error tersebut bisa dikirim ke Bela untuk diperbaiki.

## Catatan
Versi ini menghasilkan APK debug untuk penggunaan/testing. Untuk distribusi profesional, sebaiknya nanti dibuat signing/release APK dengan keystore pribadi.
