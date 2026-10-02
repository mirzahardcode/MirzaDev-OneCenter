# OneCenter

OneCenter ini aplikasi Eksperimental

## Fitur
- Dashboard utama
- Info-info terbaru
- Rilisan Aplikasi
- Private Area
- Native libraries pake C++
- Cek integritas aplikasi (biar aman dari tukang crack)
- UI navigasi ala Frosted Glass / Liquid Glass

## Tech Stack
- Kotlin
- Jetpack Compose
- C++ / JNI
- CMake
- Firebase
- Gradle

## Persyaratan
Pastikin di laptop lu udah ada:
- Android Studio
- Android SDK
- Android NDK
- CMake

## Cara Build
1. Buka project ini di Android Studio, terus tunggu aja sampe Gradle-nya selesai sync.
2. Kalo mau nge-build APK versi release, lu wajib atur konfigurasi Firebase lokal sama signing properties-nya dulu.

## Keamanan
Jangan sampe bocor! Pastiin signing keys, access tokens, akun service-account, dan file konfigurasi privat lainnya tetep aman dan ga ikut ke-commit ke repo.
