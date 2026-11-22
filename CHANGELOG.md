# Changelog

Catatan perubahan dan riwayat pembaruan aplikasi MirzaDev OneCenter.

## [1.6.5R8] - Migrasi Backend & Optimasi R8
- Migrasi pengambilan Private Documents ke Vercel Backend terotorisasi (`GET /privateDocuments` & `GET /privateDocuments?mode=authorization`) menggunakan Firebase ID Token dan header binding `X-Device-ID`.
- Pencabutan total `GITHUB_TOKEN` / PAT dari APK client Android untuk keamanan kredensial.
- Pengoptimalan R8 Code Shrinking & Minification yang memotong ukuran APK sebesar 52.55% (dari 23.09 MB menjadi 10.96 MB).
- Penerapan Keep Rules presisi untuk JNI Native Bridge C++ (`NativeAuth`, `NativeCore`, `NativeLogger`, `NativeSecurity`) dan data model Gson.
- Isolasi cache lokal per-UID (`private_docs_v3_cache_<uid>`), proteksi Account Switch (`AuthStateListener` & request generation tracking), dan propagasi `CancellationException`.

## [1.5.9] - Penyempurnaan Private Screen
Memperbarui tampilan dokumen pada halaman Private menggunakan sistem expandable card. Dokumen kini ditampilkan dalam keadaan ringkas dengan judul saja dan dapat dibuka untuk melihat isi lengkapnya.

## [1.5.8] - Peningkatan Sistem dan Keamanan
Pembaruan pada sistem keamanan dan integritas aplikasi melalui native library, serta penyempurnaan antarmuka Navigation Bar dengan mode Frosted Glass dan Liquid Glass.

## [1.5.0] - Peningkatan Native Library dan Keamanan
Mengembangkan komponen native seperti NativeCore, NativeSecurity, NativeAuth, dan NativeLogger, serta menambahkan pemeriksaan integritas aplikasi dan verifikasi tanda tangan.

## [1.4.0] - Eksperimen Liquid Glass
Memperkenalkan penyempurnaan Navigation Bar dengan tampilan eksperimental Frosted Glass dan Liquid Glass.

## [1.3.0] - Private Screen
Menghadirkan halaman Private dengan autentikasi pengguna dan sistem pengikatan perangkat.

## [1.2.0] - Integrasi APK
Menambahkan halaman APK untuk menampilkan informasi dan mengunduh aplikasi dari GitHub Releases, termasuk versi dan ukuran file.

## [1.1.0] - Penyempurnaan Antarmuka
Penyempurnaan tampilan halaman utama dan navigasi untuk memberikan pengalaman penggunaan yang lebih nyaman.

## [1.0.0] - Rilis Awal MirzaDev OneCenter
Rilis awal aplikasi MirzaDev OneCenter dengan antarmuka utama sebagai pusat akses fitur dan layanan aplikasi.