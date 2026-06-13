# Implementation Plan — Aplikasi Kasir "Mekar Sari"

**Jenis**: Native Android (Kotlin), Full Offline
**Toko**: Mekar Sari — Jl. Gatot Subroto No. 31, Cilacap

---

## 1. Tujuan & Prinsip Desain

- **Full offline**, tidak ada dependency ke internet/server
- **Startup cepat** (target <1 detik) — minim inisialisasi berat di Application class
- **UI simpel tapi enak dilihat** — fokus ke kecepatan input transaksi, bukan dekorasi
- **Cetak struk** via printer thermal Bluetooth (ESC/POS)

---

## 2. Tech Stack

| Komponen | Pilihan |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| Architecture | MVVM + Repository |
| Printer | escpos-thermalprinter-android (dantsu) |
| Navigation | Navigation Compose |
| State | StateFlow + ViewModel |

Tidak dipakai: Retrofit, WorkManager, Firebase, library analytics — semua dihindari supaya startup tetap ringan.

---

## 3. Struktur Project

```
app/src/main/java/com/mekarsari/kasir/
├── MainActivity.kt
├── KasirApp.kt (Application — minim init)
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── Product.kt
│   │   │   ├── Transaction.kt
│   │   │   ├── TransactionItem.kt
│   │   │   └── SettingEntry.kt
│   │   ├── dao/
│   │   │   ├── ProductDao.kt
│   │   │   ├── TransactionDao.kt
│   │   │   └── SettingDao.kt
│   │   └── AppDatabase.kt
│   └── repository/
│       ├── ProductRepository.kt
│       ├── TransactionRepository.kt
│       └── SettingRepository.kt
├── domain/
│   └── usecase/
│       ├── CalculateTotalUseCase.kt
│       └── CalculateChangeUseCase.kt
├── ui/
│   ├── theme/ (Color.kt, Type.kt, Theme.kt)
│   ├── navigation/AppNavHost.kt
│   ├── kasir/
│   │   ├── KasirScreen.kt
│   │   ├── KasirViewModel.kt
│   │   └── components/ (ProductGridItem, CartItemRow, PaymentSheet)
│   ├── produk/
│   │   ├── ProdukScreen.kt
│   │   ├── ProdukFormScreen.kt
│   │   └── ProdukViewModel.kt
│   ├── riwayat/
│   │   ├── RiwayatScreen.kt
│   │   ├── RiwayatDetailScreen.kt
│   │   └── RiwayatViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       └── SettingsViewModel.kt
└── printer/
    ├── BluetoothPrinterManager.kt
    └── ReceiptFormatter.kt
```

---

## 4. Database Schema

### `products`
| Field | Type | Keterangan |
|---|---|---|
| id | Int (PK, autoincrement) | |
| nama | String | |
| harga | Long | dalam Rupiah |
| stok | Int | |
| kategori | String? | opsional, untuk filter |

### `transactions`
| Field | Type | Keterangan |
|---|---|---|
| id | Int (PK, autoincrement) | |
| total | Long | |
| bayar | Long | |
| kembalian | Long | |
| metode_pembayaran | String | "cash" default |
| created_at | Long | timestamp epoch |

### `transaction_items`
| Field | Type | Keterangan |
|---|---|---|
| id | Int (PK, autoincrement) | |
| transaction_id | Int (FK) | |
| product_id | Int (FK) | |
| nama_produk_snapshot | String | snapshot nama saat transaksi |
| harga_saat_itu | Long | snapshot harga |
| qty | Int | |
| subtotal | Long | |

### `settings` (key-value)
| Field | Type | Keterangan |
|---|---|---|
| key | String (PK) | |
| value | String | |

Seed awal:
- `nama_toko` = "Mekar Sari"
- `alamat_toko` = "Jl. Gatot Subroto No. 31, Cilacap"
- `printer_mac` = "" (diisi setelah pairing)

---

## 5. Desain UI (Simpel, Cepat)

Prinsip: minim layer, minim animasi, warna kontras tinggi untuk kecepatan baca di lapangan, tombol besar (mudah ditekan jari).

### Bottom Navigation (4 tab)
1. **Kasir** (default/home)
2. **Produk**
3. **Riwayat**
4. **Settings**

### A. Kasir Screen (paling penting)
- Layout 2 kolom: kiri grid produk (LazyVerticalGrid, scroll), kanan panel keranjang
- Tap produk → otomatis masuk keranjang (qty +1)
- Search bar di atas grid produk (filter cepat by nama)
- Panel keranjang: list item + qty stepper (+/-), total otomatis update
- Tombol "Bayar" besar di bawah panel keranjang → buka bottom sheet pembayaran
- Bottom sheet pembayaran: input nominal bayar (numpad custom atau quick-amount buttons: pas, 50rb, 100rb), tampilkan kembalian real-time, tombol "Simpan & Cetak"

### B. Produk Screen
- List produk (LazyColumn) dengan nama, harga, stok
- FAB "+" → ProdukFormScreen (tambah/edit)
- Swipe atau tombol delete per item
- Form sederhana: nama, harga, stok, kategori (dropdown/opsional)

### C. Riwayat Screen
- List transaksi per hari (group by tanggal, header tanggal sticky)
- Tap item → detail transaksi (RiwayatDetailScreen) dengan list item + tombol "Cetak Ulang"

### D. Settings Screen
- Edit nama toko & alamat (tersimpan ke tabel settings)
- Bagian "Printer": tombol scan/pairing Bluetooth printer, tampilkan status koneksi
- Tombol "Test Print"

### Color & Style
- Material 3, palet warna: 1 warna primer (misal hijau/oranye khas warung), background putih/abu sangat terang
- Typography: ukuran besar untuk harga & total (mudah dibaca cepat)
- Tidak pakai animasi transisi berlebihan — default Compose transitions cukup

---

## 6. Format Struk (ESC/POS)

```
        MEKAR SARI
Jl. Gatot Subroto No. 31
       Cilacap
------------------------
[tanggal & waktu]
------------------------
Nasi Goreng     1   15.000
Es Teh          2    6.000
------------------------
Total              21.000
Bayar              25.000
Kembali             4.000
------------------------
   Terima kasih!
```

Dibuat lewat `ReceiptFormatter.kt` yang generate command ESC/POS, dikirim via `BluetoothPrinterManager`.

---

## 7. Roadmap / Urutan Kerja

| Fase | Task | Output |
|---|---|---|
| 1 | Setup project, dependencies (Compose, Room, dantsu printer lib) | Project jalan, build sukses |
| 2 | Buat entity, DAO, AppDatabase + seed settings awal | Database siap |
| 3 | Repository layer (Product, Transaction, Setting) | Data layer lengkap |
| 4 | Produk Screen (CRUD) | Bisa tambah/edit/hapus produk |
| 5 | Kasir Screen — grid produk + keranjang + hitung total | Transaksi dasar jalan (belum cetak) |
| 6 | Payment bottom sheet + simpan transaksi ke DB | Transaksi tersimpan |
| 7 | Integrasi printer (pairing + cetak struk) | Struk bisa dicetak |
| 8 | Riwayat Screen + reprint | Histori transaksi bisa dilihat & cetak ulang |
| 9 | Settings Screen (edit info toko, printer settings) | Konfigurasi lengkap |
| 10 | Optimasi startup (cek Application class, lazy init, profiling) | Startup <1 detik |
| 11 | Testing end-to-end + polish UI (spacing, warna, ukuran tombol) | Siap dipakai |

---

## 8. Catatan Tambahan

- Pre-populate beberapa produk contoh saat first launch agar testing lebih mudah
- Backup data: untuk versi awal cukup manual (export/import file .db lewat file manager), tidak wajib di MVP
- Semua harga disimpan sebagai `Long` (Rupiah, tanpa desimal) untuk hindari floating point error