# Inverter Monitor (Özel Android Uygulaması)

Orion Solar inverterinizin HC-04 Bluetooth modülüne **doğrudan** (ESP32 veya
WiFi olmadan, orijinal "SI Service Tool" uygulamasıyla aynı yöntemle) bağlanan,
sizin istediğiniz arayüzle (koyu tema, hesaplanmış akım değerleri, Bağlan/Kes
ve Ayarlar butonları) çalışan özel bir Android uygulaması.

## 🚀 En kolay yol: GitHub Actions ile otomatik APK üretimi

Android Studio kurmadan, sadece tarayıcıdan APK indirmek için:

1. [github.com](https://github.com)'da ücretsiz bir hesap açın (yoksa).
2. Sağ üstten **"New repository"** ile boş bir repo oluşturun (örn. adı
   `inverter-monitor`), **Public** veya **Private** fark etmez.
3. Bu zip'i bilgisayarınızda açın, klasörün içeriğini oluşturduğunuz repoya
   yükleyin. En kolay yol — repo sayfasında **"Add file" > "Upload files"**
   ile tüm klasörü (alt klasörler dahil) sürükleyip bırakmak. Alternatif
   olarak Git kullanıyorsanız:
   ```bash
   cd InverterMonitorApp
   git init
   git add .
   git commit -m "İlk yükleme"
   git branch -M main
   git remote add origin https://github.com/KULLANICI_ADINIZ/inverter-monitor.git
   git push -u origin main
   ```
4. Yükleme bitince repo sayfasında üstteki **"Actions"** sekmesine gidin.
   "Build APK" adında bir iş otomatik başlayacak (birkaç dakika sürer).
5. İş yeşil tik ✅ ile bitince, o işin sayfasına girip en altta
   **"Artifacts"** bölümünden **`inverter-monitor-debug-apk`** dosyasını
   indirin — bu bir zip, içinde `app-debug.apk` var.
6. `app-debug.apk`'yı telefonunuza aktarın (WhatsApp'a kendinize gönderin,
   Google Drive'a atın, USB ile kopyalayın — hangisi kolaysa) ve telefonda
   dosyaya dokunup kurun. Telefon "bilinmeyen kaynak" uyarısı verirse izin
   verin.

Kod üzerinde her değişiklik yapıp tekrar yüklediğinizde (`git push`),
Actions otomatik yeni bir APK üretir — Android Studio'ya bir daha gerek
kalmaz.

## Alternatif: Android Studio ile derleme

1. [Android Studio](https://developer.android.com/studio) kurun (ücretsiz).
2. `File > Open` ile bu klasörü (`InverterMonitorApp`) açın.
3. Gradle senkronizasyonunun bitmesini bekleyin (ilk açılışta internet gerekir,
   bağımlılıkları indirir).
4. Telefonunuzu USB ile bağlayın, "Geliştirici Seçenekleri > USB Hata Ayıklama"yı
   açın.
5. Üstteki yeşil "Run" (▶) butonuna basın — uygulama telefonunuza kurulacak.

## Kullanmadan önce

1. Telefonunuzun **Ayarlar > Bluetooth** kısmından inverterin HC-04 modülünü
   önceden eşleştirin (pairing) — uygulama sadece zaten eşleştirilmiş cihazları
   arıyor.
2. `MainActivity.kt` dosyasındaki şu satırı kontrol edin:
   ```kotlin
   private val TARGET_DEVICE_NAME_HINT = "HC-04"
   ```
   Eğer eşleştirilmiş cihazınızın Bluetooth adı farklıysa (örn. "OrionBT",
   "linvor" vb.), bu satırı güncelleyin.
3. Uygulamayı açın, Bluetooth izinlerini onaylayın, **"Bağlan"** butonuna basın.

## Notlar

- Bu uygulama tamamen yerel (offline) çalışır — internet veya WiFi gerektirmez.
- Menzil, telefonun ve HC-04'ün Bluetooth menziliyle sınırlıdır (~10-30m).
- Ayarları değiştirme (config) komutları bu sürümde yok — sadece izleme
  (telemetri okuma) yapılıyor. İsterseniz Battery Type, Charge Priority gibi
  ayarları da bu uygulamadan değiştirebilecek şekilde genişletebiliriz.
- Protokol, orijinal APK'nın decompile edilmesiyle çözüldü: cihaz `:` ile
  ayrılmış tek satırlık telemetri yayınlıyor, alan sırası koddaki yorumlarda
  belgelenmiştir.
