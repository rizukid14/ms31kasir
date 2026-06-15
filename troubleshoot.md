# Bluetooth Thermal Printer — Troubleshooting Guide

Panduan ini membantu mendiagnosis dan memperbaiki masalah koneksi Bluetooth ke printer thermal kasir (ESB, Olsera, Mokapos, Epson, Rongta, dll.) pada aplikasi Android.

---

## Prasyarat

Sebelum mulai, pastikan:

- Printer thermal sudah **di-pair** secara manual via **Settings → Bluetooth** di perangkat Android
- Bluetooth perangkat dalam kondisi **aktif**
- Printer dalam kondisi **menyala** dan **tidak terhubung ke perangkat lain**

---

## 1. Konfigurasi Manifest

Tambahkan izin berikut di `AndroidManifest.xml`:

```xml
<!-- Android 12+ (API 31+) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Android 11 ke bawah -->
<uses-permission android:name="android.permission.BLUETOOTH"
    android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
    android:maxSdkVersion="30" />
```

> **Catatan:** Tutorial lama yang hanya memakai `BLUETOOTH` dan `BLUETOOTH_ADMIN` tidak akan berfungsi di Android 12+.

---

## 2. Runtime Permission

Izin harus diminta secara eksplisit saat runtime untuk Android 12+:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ),
        REQUEST_BT_PERMISSION
    )
}
```

Tangani hasilnya di `onRequestPermissionsResult()` sebelum melanjutkan ke proses koneksi.

---

## 3. Cek Bluetooth Aktif

```kotlin
val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
val bluetoothAdapter = bluetoothManager.adapter

if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
}
```

---

## 4. Temukan Printer (Paired Devices)

```kotlin
val bondedDevices = bluetoothAdapter.bondedDevices
val printer = bondedDevices.firstOrNull {
    it.name.contains("printer", ignoreCase = true) ||
    it.name.contains("POS", ignoreCase = true)
}

if (printer == null) {
    Log.e("BT", "Printer tidak ditemukan di daftar paired devices")
}
```

> **Tip:** Log semua `bondedDevices` untuk melihat nama device printer yang sebenarnya terdaftar.

---

## 5. Buka Koneksi Socket

Gunakan UUID **Serial Port Profile (SPP)** — wajib untuk semua printer thermal Bluetooth:

```kotlin
val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

val socket: BluetoothSocket = printer.createRfcommSocketToServiceRecord(SPP_UUID)
```

> **Jangan** gunakan UUID lain. Semua printer thermal (ESB, Epson, Rongta, Bixolon, dll.) menggunakan SPP UUID ini.

---

## 6. Koneksi dan Kirim Data (Background Thread)

`socket.connect()` bersifat **blocking** — wajib dijalankan di luar main thread:

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    try {
        bluetoothAdapter.cancelDiscovery() // penting: hentikan discovery sebelum connect
        socket.connect()

        val outputStream = socket.outputStream
        outputStream.write(buildReceipt("Terima kasih!\nTotal: Rp 50.000"))
        outputStream.flush()

    } catch (e: IOException) {
        Log.e("BT", "Gagal konek: ${e.message}")
        socket.close()
    }
}
```

> **Penting:** Selalu panggil `bluetoothAdapter.cancelDiscovery()` sebelum `connect()`. Discovery yang berjalan dapat memperlambat atau menggagalkan koneksi.

---

## 7. Format Data ESC/POS

Printer thermal tidak menerima teks biasa — gunakan format **ESC/POS**:

```kotlin
fun buildReceipt(text: String): ByteArray {
    val ESC  = 0x1B.toByte()
    val GS   = 0x1D.toByte()

    val init     = byteArrayOf(ESC, 0x40)        // inisialisasi printer
    val bold_on  = byteArrayOf(ESC, 0x45, 0x01)  // bold aktif
    val bold_off = byteArrayOf(ESC, 0x45, 0x00)  // bold nonaktif
    val center   = byteArrayOf(ESC, 0x61, 0x01)  // rata tengah
    val left     = byteArrayOf(ESC, 0x61, 0x00)  // rata kiri
    val feed     = byteArrayOf(0x0A)              // line feed
    val cut      = byteArrayOf(GS, 0x56, 0x41, 0x10) // paper cut

    return init +
        center + bold_on + "STRUK PEMBELIAN\n".toByteArray() + bold_off +
        left + text.toByteArray(Charsets.UTF_8) +
        feed + feed + cut
}
```

---

## 8. Tutup Koneksi

Selalu tutup socket setelah selesai untuk mencegah memory leak:

```kotlin
try {
    socket.close()
} catch (e: IOException) {
    Log.e("BT", "Gagal menutup socket: ${e.message}")
}
```

---

## Checklist Diagnosis Cepat

| # | Yang Dicek | Cara Verifikasi |
|---|---|---|
| 1 | Izin manifest sudah API 31+? | Cek `AndroidManifest.xml` |
| 2 | Runtime permission sudah diminta? | Log hasil `requestPermissions` |
| 3 | Printer sudah di-pair di Settings? | `getBondedDevices()` ada hasilnya? |
| 4 | UUID menggunakan SPP? | `00001101-0000-1000-8000-00805F9B34FB` |
| 5 | `cancelDiscovery()` dipanggil sebelum `connect()`? | Cek urutan kode |
| 6 | `connect()` di background thread? | Pakai `Dispatchers.IO` atau `Thread` |
| 7 | Error message dicatat? | `catch (IOException e)` + `Log.e(...)` |
| 8 | Socket ditutup setelah selesai? | `socket.close()` di blok `finally` |

---

## Pesan Error Umum

| Error | Kemungkinan Penyebab | Solusi |
|---|---|---|
| `Connection refused` | UUID salah atau printer tidak support SPP | Pastikan UUID SPP, coba `createInsecureRfcommSocketToServiceRecord` |
| `Permission denied` | Runtime permission belum diberikan | Minta `BLUETOOTH_CONNECT` saat runtime |
| `Device not found` | Printer belum di-pair | Pair printer manual di Settings → Bluetooth |
| `Socket closed` | Koneksi terputus saat kirim data | Tambahkan retry logic dan cek jarak |
| `ANR / app freeze` | `connect()` di main thread | Pindahkan ke `Dispatchers.IO` |

---

## Referensi

- [Android Bluetooth Overview](https://developer.android.com/guide/topics/connectivity/bluetooth)
- [BluetoothSocket API](https://developer.android.com/reference/android/bluetooth/BluetoothSocket)
- [ESC/POS Command Reference](https://www.epson-biz.com/modules/ref_escpos/)