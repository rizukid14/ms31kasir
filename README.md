# Kasir RM. Mekar Sari (Android Native POS App)

Aplikasi kasir (Point of Sale) offline-first berbasis Android native yang dirancang khusus untuk pencatatan transaksi harian dan pencetakan struk belanja menggunakan printer thermal Bluetooth (ESC/POS) di Rumah Makan RM. Mekar Sari.

---

## 🌟 Fitur Utama

- **Offline-First (Room Database)**: Semua data produk, riwayat transaksi, dan pengaturan rumah makan disimpan secara lokal di perangkat Anda. Tidak memerlukan koneksi internet untuk beroperasi.
- **Katalog Produk & Keranjang Belanja**: Tampilan katalog grid yang responsif dengan sidebar keranjang belanja.
- **Kustomisasi Harga Transaksi**: Kasir dapat menyesuaikan harga makanan/minuman langsung di keranjang untuk transaksi tertentu tanpa memengaruhi harga master produk.
- **Sistem Perpajakan (Pajak Adjustable)**: Nilai persentase pajak dapat disesuaikan di menu Settings dan otomatis dihitung saat checkout serta dicetak di struk.
- **Pencetakan Struk Bluetooth (ESC/POS)**:
  - Integrasi printer thermal Bluetooth (ukuran kertas 58mm).
  - Pilihan **Bayar saja** (menyimpan transaksi) atau **Cetak Struk** (menyimpan dan langsung mencetak).
  - Fitur cetak ulang (*reprint*) struk langsung dari riwayat transaksi.
- **Logo Struk Kustom & Default**:
  - Pengguna dapat mengunggah gambar logo rumah makan sendiri dari galeri HP di menu Settings.
  - Jika belum mengunggah logo kustom, aplikasi otomatis mencetak logo stempel default bertuliskan **"RM. MEKAR SARI"** agar struk tetap estetik.
- **Dashboard Laporan & Statistik Bulanan**:
  - Ringkasan performa penjualan bulanan (Total Omzet, Jumlah Transaksi, dan Rata-rata Pembelian/Ticket Size).
  - Grafik batang harian (*Daily Sales Bar Chart*) yang interaktif berbasis Jetpack Compose untuk memantau tren omzet harian.
  - Daftar **5 Produk Terlaris** lengkap dengan indikator rasio penjualan berbentuk progress bar.
- **Multi-Theme (Light Mode)**: Pilihan 4 varian tema warna cerah (Orange, Biru, Hijau, Ungu) yang dapat diganti di halaman Settings dan tersimpan secara permanen.

---

## 🛠️ Arsitektur & Teknologi

- **Bahasa Pemrograman**: Kotlin
- **UI Framework**: Jetpack Compose (Modern Declarative UI)
- **Database**: Room Database (SQLite abstraction)
- **Navigation**: Jetpack Compose Navigation
- **Lifecycle & State**: JetViewModel, Kotlin Coroutines, StateFlow / Flow
- **Library Printer**: `com.github.dantsu:escpos-thermalprinter-android:3.3.0`

---

## 🚀 Cara Menjalankan & Membangun Proyek

### Persyaratan Sistem
- **Android Studio** (Koala atau versi terbaru)
- **Java JDK 17 / 21** (Sudah dibundel langsung di dalam folder instalasi Android Studio)
- **Android SDK** (Min SDK 26 / Target SDK 34)

### Langkah-Langkah Membuka Proyek
1. Clone atau unduh repositori ini ke komputer Anda.
2. Jalankan **Android Studio**.
3. Pilih **Open** dan arahkan ke folder proyek ini (`ms31-kasir`).
4. Tunggu beberapa menit hingga Gradle mengunduh (*sync*) seluruh dependensi proyek secara otomatis.
5. Hubungkan HP Android asli Anda via Kabel Data (pastikan USB Debugging aktif) atau jalankan Emulator.
6. Klik tombol **Run** (ikon segitiga hijau) di toolbar atas Android Studio untuk menginstal aplikasi di perangkat Anda.

### Cara Membuat File APK
1. Di menu atas Android Studio, pilih **Build** ➔ **Build Bundle(s) / APK(s)** ➔ **Build APK(s)**.
2. Setelah proses selesai, klik opsi **locate** pada notifikasi gelembung di pojok kanan bawah untuk mengambil file `.apk` Anda yang berada di folder:
   `app/build/outputs/apk/debug/app-debug.apk`
