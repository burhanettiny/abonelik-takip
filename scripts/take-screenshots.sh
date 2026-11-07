#!/usr/bin/env bash
# Play Store ekran görüntülerini emülatörden alır. CI'da screenshots.yml tarafından çağrılır;
# yerelde de açık bir emülatör/cihaz varken çalıştırılabilir.
set -euo pipefail

PKG=com.aboneliktakip.app.debug
ACT=com.aboneliktakip.app.MainActivity
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=screenshots

adb wait-for-device
adb install -r -g "$APK"
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true

# Temiz durum çubuğu (saat 09:41, tam pil, bildirim yok)
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4
demo network -e mobile show -e datatype none -e level 4
demo notifications -e visible false

shot() { # locale theme route name
  local locale=$1 theme=$2 route=$3 name=$4
  adb shell cmd uimode night "$theme" >/dev/null
  adb shell am force-stop "$PKG"
  adb shell cmd locale set-app-locales "$PKG" --locales "$locale"
  adb shell am start -W -n "$PKG/$ACT" --ez demo_seed true --es demo_route "$route" >/dev/null
  sleep 4
  mkdir -p "$OUT/$locale"
  adb exec-out screencap -p > "$OUT/$locale/$name.png"
  echo "ok $OUT/$locale/$name.png"
}

for locale in tr-TR en-US; do
  shot "$locale" no  home     1_home
  shot "$locale" no  stats    2_stats
  shot "$locale" no  edit     3_edit
  shot "$locale" yes home     4_home_dark
  shot "$locale" no  settings 5_settings
done

adb shell cmd uimode night no >/dev/null
demo exit
