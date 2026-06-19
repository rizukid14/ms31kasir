# Redesain Antarmuka Kasir Mekar Sari (Premium & Professional POS Look)

Melakukan perombakan total visual (redesain) pada seluruh layar aplikasi **Kasir Mekar Sari** agar sepenuhnya menyamai mockup **Stitch Redesign** (header hijau brand, grid produk 2 kolom dengan gambar/badge terpilih, dan checkout bar yang disempurnakan) dengan mempertahankan **Bottom Navigation Bar** (diredesain premium), mengunduh gambar makanan dari **Unsplash API** ke folder `food_images` di root project, serta mengimplementasikan kode aplikasi menggunakan **Premium Gradient Placeholders** terlebih dahulu agar siap diintegrasikan nanti.

---

## User Review Required

> [!IMPORTANT]
> - **Unduh Gambar ke Root (`food_images`)**: Kita akan menggunakan Kunci Akses Unsplash milik Anda (`CrxjE1fJ6SXHpNyIeXm7qqaey_pS-DCxra1ja2-TBkg`) untuk mengunduh 14 kategori gambar makanan/minuman beresolusi tinggi yang representatif langsung ke folder `c:\Data (D)\Vibecoding\mekarsari-kasir\food_images`. Folder ini bertindak sebagai galeri aset mentah yang siap Anda implementasikan ke folder `res/drawable` nanti.
> - **Premium Gradient Placeholders di Kode**: Di dalam kode `KasirScreen.kt`, slot gambar pada card produk akan menggunakan visual placeholder berupa gradient warna premium yang disesuaikan secara dinamis (menggunakan hash nama produk) beserta ikon representatif (`Restaurant` untuk makanan, `LocalDrink` untuk minuman). Ini membuat tampilan langsung terlihat mewah saat ini juga.
> - **Bottom Navigation Bar Redesign**: Navigasi tetap menggunakan Bottom Bar, diredesain agar tampil modern dengan indicator pill halus, teks tebal, dan border top tipis `BorderGray`.
> - **Grid Produk 2 Kolom**: Daftar menu vertikal (`CompactProductRow`) diubah menjadi grid 2 kolom berisi card produk (`ProductGridCard`) dengan placeholder gambar, label harga, status stok, dan badge jumlah terpilih ("1 Added" dsb.) di pojok kanan atas.

---

## Proposed Changes

### 1. Download Asset ke Root

- Menulis skrip PowerShell/Python untuk mengunduh 14 kategori gambar makanan dari Unsplash API dengan Access Key Anda dan menyimpannya di folder `c:\Data (D)\Vibecoding\mekarsari-kasir\food_images`:
  - `food_sate.jpg`, `food_soup.jpg`, `food_chicken.jpg`, `food_fish.jpg`, `food_egg.jpg`, `food_vegetables.jpg`, `food_rice.jpg`, `food_tofu.jpg`, `food_chili.jpg`, `drink_iced_tea.jpg`, `drink_orange.jpg`, `drink_soda.jpg`, `drink_water.jpg`, `drink_coffee.jpg`

---

### 2. Redesain Bottom Navigation Bar (AppNavHost)

#### [MODIFY] [AppNavHost.kt](file:///c:/Data%20(D)/Vibecoding/mekarsari-kasir/app/src/main/java/com/mekarsari/kasir/ui/navigation/AppNavHost.kt)
- Mempertahankan `bottomBar` di `Scaffold` tetapi mendesain ulangnya agar terlihat premium:
  - Menggunakan container dengan warna background putih bersih (`surface`), dengan border atas tipis `1.dp` berwarna `BorderGray`.
  - Menyesuaikan warna indicator pill `NavigationBarItem` agar menggunakan warna `primary` dengan tingkat transparansi yang pas saat dipilih, serta teks yang tebal (`FontWeight.Bold`).

---

### 3. Redesain Kasir Screen (KasirScreen)

#### [MODIFY] [KasirScreen.kt](file:///c:/Data%20(D)/Vibecoding/mekarsari-kasir/app/src/main/java/com/mekarsari/kasir/ui/kasir/KasirScreen.kt)
- **Header Hijau Premium**:
  - Menampilkan header dengan tinggi `72.dp` berwarna Forest Green (`0xFF1B5E20` atau primer aktif).
  - Teks brand: "RM. Mekar Sari" berwarna putih.
  - Tombol aksi: Tombol cari berbentuk lingkaran dan dropdown status "Dine In" dengan background aksen hijau/putih.
- **Header Kategori**:
  - Tampilan teks kategori (misal: "Makanan Utama", "Minuman Segar") dengan bold gelap, badge jumlah item (misal: "12 Items"), dan chevron kanan untuk expand/collapse.
- **Grid Produk 2 Kolom**:
  - Mengubah `LazyColumn` yang sebelumnya merender satu item baris menjadi chunking `chunked(2)` sehingga merender 2 `ProductGridCard` per baris.
  - Setiap card memiliki shape `16.dp` dengan border tipis dan background putih.
  - Bagian atas card adalah slot gambar (`120.dp` height) yang menampilkan premium gradient dinamis dengan inisial nama menu dan ikon makanan/minuman yang relevan.
  - Jika item ditambahkan ke keranjang, tampilkan badge hijau gelap bertuliskan `"X Added"` (misal: `"1 Added"`) di sudut kanan atas gambar dan beri border hijau tebal di sekeliling card.
- **Checkout Bar Bawah**:
  - Mengubah floating card melayang di bawah menjadi bar checkout yang membentang penuh di bagian bawah layar.
  - Menampilkan subtotal, pajak (10%), dan Total Harga format besar (`Rp 85.800`).
  - Menyediakan tombol "Clear" berlambang tempat sampah dan tombol "Review Checkout" berwarna kuning-emas (`Color(0xFFFFB300)`) dengan ikon keranjang belanja untuk memicu PaymentBottomSheet.

---

## Rencana Verifikasi

### Verifikasi Otomatis
- Menjalankan `./gradlew compileDebugKotlin` untuk memastikan tidak ada kesalahan kompilasi Kotlin.
- Menjalankan `./gradlew assembleDebug` untuk memvalidasi proses build Android selesai dengan sukses.

### Verifikasi Manual
- Menjalankan aplikasi di emulator/perangkat dan mencocokkan layout persis dengan gambar mockup Stitch.
- Memastikan navigasi bottom bar berfungsi lancar untuk berpindah antar-layar.
- Memverifikasi placeholder gradien muncul dengan rapi di card produk, dan badge terpilih bertambah/berkurang sesuai interaksi klik.
