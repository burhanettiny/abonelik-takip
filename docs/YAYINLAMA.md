# Google Play'de Yayınlama Rehberi — Abonelik Takip

Bu rehber, uygulamayı sıfırdan Google Play'de yayına almak için gereken her adımı ve Play Console'daki
formlara verilecek **hazır cevapları** içerir. Sırasıyla ilerleyin.

---

## 0. Özet kontrol listesi

| # | Adım | Durum |
|---|------|-------|
| 1 | Google Play geliştirici hesabı (tek seferlik 25 $) | ☐ |
| 2 | Yükleme anahtarı (upload keystore) oluşturma | ☐ |
| 3 | GitHub secret'larını ekleme | ☐ |
| 4 | `v1.0.0` etiketiyle imzalı AAB üretme | ☐ |
| 5 | Gizlilik politikası sayfasının yayında olduğunu doğrulama | ☐ |
| 6 | Play Console'da uygulama oluşturma | ☐ |
| 7 | Mağaza girişi (metinler + görseller) | ☐ |
| 8 | Uygulama içeriği formları (gizlilik, veri güvenliği, içerik derecelendirme…) | ☐ |
| 9 | Kapalı test (yeni kişisel hesaplarda zorunlu) | ☐ |
| 10 | Üretim (production) sürümü | ☐ |

Uygulamanın teknik bilgileri:

| | |
|---|---|
| Paket adı (applicationId) | `com.aboneliktakip.app` — **yayından sonra değiştirilemez** |
| Sürüm | `versionName 1.0.0`, `versionCode 1` |
| minSdk / targetSdk / compileSdk | 26 (Android 8.0) / 36 / 37 |
| İzinler | Yalnızca `POST_NOTIFICATIONS` (+ WorkManager'ın eklediği sistem izinleri). **İnternet izni yok.** |
| Gizlilik politikası URL'si | https://burhanettiny.github.io/abonelik-takip/privacy.html |

> Paket adını değiştirmek isterseniz şimdi değiştirin: `app/build.gradle.kts` içindeki `applicationId`.
> `namespace` değişmek zorunda değildir.

---

## 1. Geliştirici hesabı

1. https://play.google.com/console adresine gidin, Google hesabınızla giriş yapın.
2. Hesap türü: **Kişisel** (şirketiniz yoksa).
3. 25 $ ücreti ödeyin, kimlik doğrulamasını tamamlayın (kimlik belgesi + telefon doğrulaması istenir; onay birkaç gün sürebilir).
4. Geliştirici profilinde herkese açık bir **iletişim e-postası** belirleyin (mağazada görünür).

---

## 2. Yükleme anahtarı (upload key)

Google Play, uygulamanızı kendi **uygulama imzalama anahtarıyla** imzalar (Play App Signing). Siz yalnızca
yükleme anahtarıyla imzalarsınız. Bu anahtarı kaybederseniz Play Console üzerinden sıfırlatabilirsiniz, ama
yine de **mutlaka yedekleyin**.

Bilgisayarınızda (JDK kurulu olmalı; Android Studio ile birlikte gelir) proje klasöründe:

```bash
./scripts/create-upload-keystore.sh
```

Bu betik:
- `upload-keystore.jks` dosyasını (RSA 4096, 10 000 gün geçerli) oluşturur,
- `keystore.properties` dosyasını yazar (Gradle buradan okur),
- iki dosya da `.gitignore`'dadır; **asla depoya göndermeyin**.

Windows'ta betik çalışmazsa Android Studio'da: **Build > Generate Signed App Bundle or APK > Android App Bundle > Create new…**
Alias olarak `upload` kullanın ve ardından `keystore.properties` dosyasını elle oluşturun:

```properties
storeFile=upload-keystore.jks
storePassword=ŞİFRENİZ
keyAlias=upload
keyPassword=ŞİFRENİZ
```

Artık yerelde imzalı paket üretebilirsiniz:

```bash
./gradlew bundleRelease
```

Çıktı: `app/build/outputs/bundle/release/app-release.aab`

---

## 3. GitHub'da otomatik imzalı derleme (önerilir)

Depodaki **Settings > Secrets and variables > Actions > New repository secret** bölümüne şu dört değeri ekleyin:

| Secret adı | Değer |
|---|---|
| `ABONE_KEYSTORE_BASE64` | `base64 -w0 upload-keystore.jks` çıktısı (macOS: `base64 -i upload-keystore.jks`; Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks"))`) |
| `ABONE_KEYSTORE_PASSWORD` | keystore şifresi |
| `ABONE_KEY_ALIAS` | `upload` |
| `ABONE_KEY_PASSWORD` | anahtar şifresi |

Sonra bir sürüm etiketi gönderin:

```bash
git tag v1.0.0
```

```bash
git push origin v1.0.0
```

**Actions > Release** iş akışı testleri ve lint'i çalıştırır, imzalı `app-release.aab` + `app-release.apk` üretir,
imzayı doğrular ve **Releases** sayfasında bir sürüm oluşturur. `mapping.txt` dosyası da eklenir (Play Console'a
yüklerseniz çökme raporları okunabilir olur).

Sonraki sürümlerde `app/build.gradle.kts` içinde `versionCode`'u **1 artırın** (2, 3, …) ve `versionName`'i güncelleyin
(ör. `1.0.1`), ardından yeni etiket gönderin (`v1.0.1`).

---

## 4. Gizlilik politikası

Play, gizlilik politikası URL'si ister. Bu depo GitHub Pages ile `docs/` klasörünü yayınlar:

- Ana sayfa: https://burhanettiny.github.io/abonelik-takip/
- Gizlilik politikası: https://burhanettiny.github.io/abonelik-takip/privacy.html

Sayfa açılmıyorsa: depo **Settings > Pages > Build and deployment > Source: Deploy from a branch**,
Branch: `main`, klasör: `/docs` seçip kaydedin. Birkaç dakika içinde yayına girer.

---

## 5. Play Console'da uygulama oluşturma

**Tüm uygulamalar > Uygulama oluştur**

| Alan | Değer |
|---|---|
| Uygulama adı | `Abonelik Takip: Fatura & Ödeme` |
| Varsayılan dil | Türkçe – tr-TR |
| Uygulama mı, oyun mu? | Uygulama |
| Ücretsiz mi, ücretli mi? | Ücretsiz |
| Beyanlar | Geliştirici Program Politikaları ve ABD ihracat yasaları kutularını işaretleyin |

---

## 6. Mağaza girişi (Ana mağaza girişi)

Tüm metin ve görseller depoda hazırdır:

| Play Console alanı | Dosya (tr-TR) | Sınır |
|---|---|---|
| Uygulama adı | `fastlane/metadata/android/tr-TR/title.txt` | 30 |
| Kısa açıklama | `fastlane/metadata/android/tr-TR/short_description.txt` | 80 |
| Tam açıklama | `fastlane/metadata/android/tr-TR/full_description.txt` | 4000 |
| Uygulama simgesi (512×512 PNG) | `fastlane/metadata/android/tr-TR/images/icon.png` | |
| Öne çıkan grafik (1024×500) | `fastlane/metadata/android/tr-TR/images/featureGraphic.png` | |
| Telefon ekran görüntüleri (2–8 adet) | `fastlane/metadata/android/tr-TR/images/phoneScreenshots/` | |

İngilizce çeviri eklemek için: **Mağaza girişi > Çevirileri yönet > Kendi çevirilerimi ekle > İngilizce (ABD) – en-US**
ve `en-US` klasöründeki dosyaları kullanın.

Diğer alanlar:

| Alan | Değer |
|---|---|
| Uygulama kategorisi | **Finans** |
| Etiketler | Bütçe, Kişisel finans, Fatura hatırlatıcı (önerilenlerden seçin) |
| E-posta | Geliştirici iletişim e-postanız |
| Web sitesi | https://burhanettiny.github.io/abonelik-takip/ |

> Ekran görüntülerini yeniden üretmek için: **Actions > Store screenshots > Run workflow**. İş bitince
> `screenshots` artifact'ını indirip ilgili klasörlere koyun.

---

## 7. Uygulama içeriği (Politika > Uygulama içeriği)

### Gizlilik politikası
`https://burhanettiny.github.io/abonelik-takip/privacy.html`

### Uygulama erişimi
**Tüm işlevler özel erişim gerektirmeden kullanılabilir** (giriş/hesap yok).

### Reklamlar
**Hayır, uygulamamda reklam yok.**

### İçerik derecelendirmesi (IARC anketi)
- E-posta adresinizi girin, kategori: **Yardımcı Program, Verimlilik, İletişim veya Diğer** (All Other App Types).
- Şiddet, cinsellik, küfür, uyuşturucu, kumar vb. tüm sorulara: **Hayır**.
- Kullanıcılar arası etkileşim / içerik paylaşımı: **Hayır**.
- Konum paylaşımı: **Hayır**. Dijital ürün satın alma: **Hayır**.
- Beklenen sonuç: **3+ / Herkes (PEGI 3, ESRB Everyone)**.

### Hedef kitle ve içerik
- Hedef yaş grupları: **18 ve üzeri** (finans uygulaması olduğu için önerilir; 13+ da seçilebilir — 13 yaş altını **seçmeyin**, aksi hâlde Aileler politikası devreye girer).
- Uygulama çocukların ilgisini çekebilir mi? **Hayır**.

### Veri güvenliği (Data safety)
| Soru | Cevap |
|---|---|
| Uygulamanız, gerekli kullanıcı veri türlerinden herhangi birini topluyor veya paylaşıyor mu? | **Hayır** |
| (Bu cevapla sonraki sorular atlanır) | |

Gerekçe: Tüm veriler yalnızca cihazda saklanır; uygulamanın internet izni yoktur. Google'ın tanımına göre
cihazdan çıkmayan veri "toplanan veri" sayılmaz. Kullanıcının kendi başlattığı JSON yedeği de toplama sayılmaz.

### Devlet uygulamaları / Finansal özellikler / Sağlık
- Devlet uygulaması: **Hayır**
- Finansal özellikler beyanı: **"Uygulamam bu finansal özelliklerden hiçbirini sunmuyor"** (kredi, ödeme işleme, kripto vb. yok; yalnızca kişisel takip).
- Sağlık: **Hayır**

### Haber uygulaması: **Hayır**

---

## 8. Kapalı test (yeni kişisel hesaplar için zorunlu)

2023 sonrasında açılan **kişisel** geliştirici hesaplarında üretime çıkmadan önce kapalı test şartı vardır
(şu anki kural: **en az 12 test kullanıcısı, kesintisiz 14 gün**). Güncel koşulları Play Console'daki
**Kontrol paneli** adımlarında görürsünüz.

1. **Test > Kapalı test > Kanal oluştur** (ör. "Beta").
2. **Test kullanıcıları**: bir e-posta listesi oluşturun ve en az 12 Gmail adresi ekleyin (aile, arkadaşlar).
3. **Yeni sürüm oluştur** → `app-release.aab` dosyasını yükleyin → sürüm notu: `changelogs/1.txt` içeriği.
4. Sürümü incelemeye gönderin. Onay sonrası test bağlantısını kullanıcılara iletin; **katılmaları ve uygulamayı yüklemeleri** gerekir.
5. 14 gün boyunca test kullanıcılarının katılımda kalmasını sağlayın.
6. Süre dolunca **Kontrol paneli > Üretim erişimine başvur** formunu doldurun (test sürecini, aldığınız geri bildirimleri ve yaptığınız iyileştirmeleri kısaca anlatın).

---

## 9. Üretim sürümü

1. **Üretim > Ülkeler/bölgeler**: Türkiye (ve istediğiniz diğer ülkeler).
2. **Yeni sürüm oluştur** → aynı AAB'yi (veya `versionCode`'u artırılmış yenisini) yükleyin.
3. **İncelemeye gönder**. İlk inceleme genellikle 1–7 gün sürer.

---

## 10. Sık karşılaşılan red sebepleri ve önlemleri

| Risk | Bu projede durum |
|---|---|
| Hedef API seviyesi düşük | targetSdk 36 ✔ |
| Gizlilik politikası eksik/erişilemez | GitHub Pages'te, uygulama içinden de bağlantı var ✔ |
| Veri güvenliği formu ile davranış uyumsuz | Uygulama veri toplamıyor, internet izni yok ✔ |
| Başkasına ait marka/logo kullanımı | Simge ve görseller özgün; ekran görüntülerinde marka adı yok ✔ |
| Yanıltıcı açıklama | Açıklamalar yalnızca mevcut özellikleri anlatıyor ✔ |
| Çökme / ANR | Birim testleri + lint CI'da her gönderimde çalışıyor ✔ |
| Bildirim izni gerekçesi | İzin yalnızca Android 13+'ta, ilk açılışta bir kez isteniyor ✔ |

---

## 11. Sonraki sürümler için

1. `app/build.gradle.kts` → `versionCode` +1, `versionName` güncelle.
2. `fastlane/metadata/android/*/changelogs/<versionCode>.txt` dosyası ekle.
3. `git tag vX.Y.Z` ve `git push origin vX.Y.Z` → Releases'tan AAB'yi indir → Play Console'a yükle.
