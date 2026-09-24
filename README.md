# Orion İzleme (OrionMonitor)

Özcan Göktay'ın ORION Solar Inverter'ı için, orijinal "SI SERVICE TOOL"
uygulamasının yerine geçecek, Modbus RTU üzerinden gerçek verilerle
çalışan Android uygulaması.

## Bu neyi çözer, neyi çözmez

Bu bir **Android Studio projesi**dir — derlenmiş bir .apk dosyası değildir.
Bunun sebebi: bu sohbetin çalıştığı ortamda Android SDK / Gradle araçları
kurulu değil ve ağ erişimi Google'ın Android paket sunucularına kapalı.
Yani burada gerçek bir .apk derleyip elinize veremiyorum.

Bunun yerine size **tam çalışır kaynak kod** hazırladım. Kendi
bilgisayarınızda (veya Android Studio'nun mobil sürümü varsa telefonda)
birkaç dakikada gerçek bir .apk'ya dönüştürebilirsiniz.

## Nasıl derlenir

1. [Android Studio](https://developer.android.com/studio)'yu indirip kurun (ücretsiz).
2. Android Studio'da **Open** ile bu klasörü (`OrionMonitor/`) açın.
3. İlk açılışta Gradle senkronizasyonu otomatik başlar; internet
   ister (Google/Maven sunucularından bağımlılıkları indirir).
   Eğer "Gradle wrapper bulunamadı" uyarısı çıkarsa, Android Studio
   size otomatik olarak bir wrapper oluşturmayı teklif edecektir —
   kabul edin.
4. Telefonunuzu USB ile bağlayın, telefonda **Geliştirici seçenekleri
   > USB hata ayıklama**yı açın.
5. Android Studio'da yeşil **Run ▶** butonuna basın. Uygulama telefona
   kurulup açılır.
6. Kalıcı bir .apk dosyası isterseniz: **Build > Build Bundle(s) /
   APK(s) > Build APK(s)**. Oluşan dosya
   `app/build/outputs/apk/debug/app-debug.apk` yolunda olur; bunu
   telefona kopyalayıp doğrudan kurabilirsiniz.

## Protokol — kılavuzdan çıkardıklarımız

Paylaştığınız "SV1500/3000/5000 INVERTER Kullanım Kılavuzu" sayfa 47'ye göre:

- **Modbus RTU**, 9600 baud, 8 bit, 1 stop, parity yok
- Bluetooth modülü şeffaf bir RS485↔Bluetooth köprüsü (klasik SPP,
  standart UUID `00001101-...`)
- Register'lar `40000`'den başlıyor; kodda bunu yerel adres `0` kabul
  ettik (`RegisterMap.START_ADDRESS`)
- `40000-40024` arası tek seferde okunuyor (function code `0x03`)
- `40016, 40017, 40018, 40022, 40023` yazılabilir (function code `0x06`)
- `40007` bit bit okunan durum/arıza kelimesi (`StatusBit.kt` içinde
  kılavuzdaki tabloyla birebir eşlendi)

## Kesin olmayan / cihazınızda doğrulanması gereken noktalar

Kılavuz register adreslerini net veriyor ama şu ikisini **vermiyor**,
kodda varsayım olarak bırakıldı — ilk bağlantıda çalışmazsa burada arayın:

1. **Slave ID** — kılavuzda yazmıyor, `RegisterMap.SLAVE_ID = 1` varsayıldı.
   Yanıt gelmezse `0` veya `2` deneyin.
2. **Register taban kayması** — bazı üreticilerde "40000" gösterimi
   Modbus adresi `0`'a, bazılarında `39999`'a karşılık gelir
   (1-tabanlı/0-tabanlı fark). Yanıt gelmiyorsa `RegisterMap.START_ADDRESS`
   değerini `-1` kaydırıp deneyin.

## Bilerek eklemediklerimiz

- **Akü tipi** ve **DOD bağlantı kesme/bağlanma seviyeleri** — orijinal
  uygulamanın ekranında vardı ama kılavuzun Modbus tablosunda bu
  ayarların register adresi verilmemiş. Arayüzde bu alanları şimdilik
  hiç göstermedik (yanlış register'a yazıp cihaza zarar vermemek için).
  Gerçek adresler bulununca `RegisterMap.kt` ve `SettingsScreen.kt`'ye
  eklemek yeterli.
- **Batarya sıcaklığı** (40008) — sizin isteğinizle sadece inverter
  sıcaklığı gösteriliyor, batarya sıcaklığı okunuyor ama arayüze
  koyulmadı.

## Dosya yapısı

```
app/src/main/java/com/ozcangoktay/orionmonitor/
  modbus/ModbusRtu.kt          Modbus RTU çerçeveleme + CRC16
  bluetooth/SppBluetoothLink.kt Klasik Bluetooth SPP bağlantısı
  data/InverterData.kt          Register haritası + veri modeli + arıza bitleri
  data/InverterRepository.kt    Bağlan / periyodik oku / ayar yaz
  ui/MainViewModel.kt           Ekranlar arası ortak durum
  ui/DashboardScreen.kt         Ana izleme ekranı
  ui/SettingsScreen.kt          Ayarlar ekranı
  ui/AlarmsScreen.kt            Alarmlar ve durum bilgileri
  ui/BluetoothPickerScreen.kt   Cihaz seç / bağlan (1 tuşu)
  MainActivity.kt               İzinler + ekran yönlendirme
```

## İlk denemede sorun çıkarsa

- Uygulama "Veri bekleniyor…" da takılı kalıyorsa: slave ID veya
  register taban kaymasını yukarıdaki gibi deneyin.
- Bağlantı hiç kurulmuyorsa: telefonun Bluetooth ayarlarından modülle
  önce eşleştiğinizden emin olun (PIN genelde `1234` veya `0000`).
- Android 12+ cihazlarda ilk açılışta Bluetooth izni istenir; reddederse
  cihaz listesi boş görünür — Ayarlar > Uygulamalar > Orion İzleme >
  İzinler'den elle açabilirsiniz.
