#!/usr/bin/env bash
# R8 ile küçültülmüş release APK'sını emülatörde çalıştırır ve çökme olmadığını doğrular.
set -euo pipefail
PKG=com.aboneliktakip.app
ACT=$PKG/.MainActivity
APK=app/build/outputs/apk/release/app-release.apk

adb wait-for-device
adb install -r -g "$APK"
adb logcat -c

check_alive() {
  sleep 3
  if ! adb shell pidof "$PKG" >/dev/null; then
    echo "::error::Uygulama çöktü ($1)"
    adb logcat -d -b crash || true
    adb logcat -d | grep -E "AndroidRuntime|FATAL" -A 30 | head -120 || true
    exit 1
  fi
  echo "ok: $1"
}

adb shell am start -W -n "$ACT" >/dev/null
check_alive "ilk açılış (boş liste)"

# Ekran boyutuna göre dokunuşlarla gezin.
read -r W H < <(adb shell wm size | awk -F'[ x]' '/Physical/{print $3, $4}')
tap() { adb shell input tap "$1" "$2"; sleep 2; }

tap $((W*85/100)) $((H*86/100))         # "Ekle" FAB -> ekleme ekranı
check_alive "ekleme ekranı"
adb shell input keyevent KEYCODE_BACK    # değişiklik yok -> listeye dön
sleep 2

for x in 50 83; do                      # alt menü: Analiz, Ayarlar
  tap $((W*x/100)) $((H*96/100))
  check_alive "sekme x=$x"
done
tap $((W*17/100)) $((H*96/100))
check_alive "abonelikler sekmesi"

# Bildirimden açılış (var olmayan kayıt -> sessizce kapanmalı)
adb shell am start -W -n "$ACT" --el subscription_id 999 >/dev/null
check_alive "bildirimden derin bağlantı"

# Hatırlatma işçisini zorla çalıştır
adb shell cmd jobscheduler run -f "$PKG" 0 >/dev/null 2>&1 || true
check_alive "arka plan işi"

if adb logcat -d | grep -q "FATAL EXCEPTION"; then
  echo "::error::Logda FATAL EXCEPTION bulundu"
  adb logcat -d | grep "FATAL EXCEPTION" -A 30 | head -80
  exit 1
fi
echo "Release duman testi başarılı."
