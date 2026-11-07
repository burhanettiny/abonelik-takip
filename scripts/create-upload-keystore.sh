#!/usr/bin/env bash
# Google Play "yükleme anahtarı" (upload key) oluşturur ve keystore.properties dosyasını yazar.
# Kullanım: ./scripts/create-upload-keystore.sh
# ÖNEMLİ: Oluşan .jks dosyasını ve şifreleri güvenli bir yerde (ör. şifre yöneticisi) yedekleyin.
#         Bu dosyalar .gitignore'dadır; asla depoya göndermeyin.
set -euo pipefail
cd "$(dirname "$0")/.."

KEYSTORE=upload-keystore.jks
ALIAS=upload

if [ -f "$KEYSTORE" ]; then
  echo "$KEYSTORE zaten var; üzerine yazılmadı." >&2
  exit 1
fi

read -rsp "Keystore şifresi (en az 6 karakter): " STOREPASS; echo
read -rsp "Şifre (tekrar): " STOREPASS2; echo
[ "$STOREPASS" = "$STOREPASS2" ] || { echo "Şifreler eşleşmiyor." >&2; exit 1; }
read -rp "Ad Soyad (sertifikada görünür): " CN

keytool -genkeypair -v \
  -keystore "$KEYSTORE" -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STOREPASS" -keypass "$STOREPASS" \
  -dname "CN=$CN, C=TR"

cat > keystore.properties <<PROPS
storeFile=$KEYSTORE
storePassword=$STOREPASS
keyAlias=$ALIAS
keyPassword=$STOREPASS
PROPS
chmod 600 keystore.properties "$KEYSTORE"

echo
echo "Oluşturuldu: $KEYSTORE ve keystore.properties"
echo "GitHub Actions için base64 değeri (ABONE_KEYSTORE_BASE64 secret'ı):"
echo "  base64 -w0 $KEYSTORE   (macOS: base64 -i $KEYSTORE)"
