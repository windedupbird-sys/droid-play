#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE_DIR="$ROOT_DIR/release-keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/colortap-release.keystore"
PROPS_FILE="$ROOT_DIR/keystore.properties"

mkdir -p "$KEYSTORE_DIR"

if [[ -f "$KEYSTORE_FILE" ]]; then
  echo "Keystore already exists at $KEYSTORE_FILE"
  exit 0
fi

STORE_PASSWORD="${STORE_PASSWORD:-$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 24)}"
KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -alias colortap \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=Color Tap, OU=Mobile, O=Color Tap Game"

cat > "$PROPS_FILE" <<EOF
storeFile=release-keystore/colortap-release.keystore
storePassword=$STORE_PASSWORD
keyAlias=colortap
keyPassword=$KEY_PASSWORD
EOF

chmod 600 "$PROPS_FILE" "$KEYSTORE_FILE"

echo "Created release keystore:"
echo "  $KEYSTORE_FILE"
echo "  $PROPS_FILE"
echo ""
echo "Back up these files securely. You need the same key for all Play Store updates."
echo ""
echo "For GitHub Actions, add these repository secrets:"
echo "  KEYSTORE_BASE64=$(base64 -w 0 "$KEYSTORE_FILE")"
echo "  KEYSTORE_PASSWORD=$STORE_PASSWORD"
echo "  KEY_ALIAS=colortap"
echo "  KEY_PASSWORD=$KEY_PASSWORD"
