# Abonelik Takip

Netflix, Spotify, fatura, kira gibi düzenli ödemeleri takip eden, ödeme gününden önce hatırlatan
ve aylık/yıllık toplam harcamayı gösteren Android uygulaması.

## Özellikler (v1.0)
- Abonelik ekleme / düzenleme / silme
- Haftalık, aylık, 3 aylık, yıllık ödeme sıklığı
- TRY, USD, EUR para birimleri
- Bir sonraki ödeme tarihinin otomatik hesaplanması (ay sonu günleri korunur)
- Aylık, yıllık ve önümüzdeki 30 günlük toplamlar
- Ödemeden 0/1/3/7 gün önce bildirim
- Tamamen çevrimdışı; veriler yalnızca cihazda (Room veritabanı)
- Material 3, koyu tema ve Android 12+ dinamik renk desteği

## Teknoloji
Kotlin · Jetpack Compose (Material 3) · Room · WorkManager · minSdk 26 (Android 8.0)

## Çalıştırma
1. Depoyu klonla ve **Android Studio**'da aç (`File > Open`).
2. Gradle senkronizasyonu bitince bir emülatör veya USB hata ayıklaması açık telefon seç, **Run ▶**.
3. Birim testleri: `./gradlew test`

> Android Studio sürüm yükseltmesi önerirse (AGP Upgrade Assistant) kabul edebilirsin.

## Play Store'a yükleme kontrol listesi
- [ ] `applicationId` son hâline karar ver (`app/build.gradle.kts`) — yayından sonra değiştirilemez
- [ ] **targetSdk**: Google Play'in güncel hedef API şartını kontrol et ve gerekirse `compileSdk`/`targetSdk` değerini yükselt
- [ ] Yükleme anahtarı (keystore) oluştur: `Build > Generate Signed App Bundle` — keystore'u **asla** depoya koyma
- [ ] `./gradlew bundleRelease` ile `.aab` üret
- [ ] Gizlilik politikası: `PRIVACY.md` içeriğini herkese açık bir URL'de yayınla (ör. GitHub Pages)
- [ ] Mağaza görselleri: 512×512 simge, 1024×500 öne çıkan görsel, en az 2 ekran görüntüsü
- [ ] Veri güvenliği formu: "Veri toplanmıyor / paylaşılmıyor"
- [ ] Yeni kişisel hesaplar için kapalı test şartı (Play Console'da güncel koşulları kontrol et)

## Yol haritası
- [ ] Kategori ve renk seçimi
- [ ] Döviz kuru ile tek para biriminde toplam
- [ ] Ana ekran widget'ı
- [ ] Yedekle / geri yükle (JSON dışa aktarma)
- [ ] Pro sürüm (uygulama içi satın alma)
