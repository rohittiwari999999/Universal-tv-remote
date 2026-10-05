#!/bin/bash
# ==============================================================================
# Script: Generate Upload Keystore and key.properties for Google Play Publishing
# ==============================================================================

set -e

echo "=========================================================="
echo " Universal TV Remote - Keystore & Release Signing Generator"
echo "=========================================================="

KEYSTORE_DIR="./android/app"
KEYSTORE_PATH="$KEYSTORE_DIR/upload-keystore.jks"
PROPERTIES_FILE="./android/key.properties"

# Ensure android/app directory exists
mkdir -p "$KEYSTORE_DIR"

if [ -f "$KEYSTORE_PATH" ]; then
    echo "⚠️  Keystore already exists at $KEYSTORE_PATH"
else
    echo "🔑 Generating new production upload keystore..."
    keytool -genkey -v \
        -keystore "$KEYSTORE_PATH" \
        -storetype JKS \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -alias upload \
        -storepass "RemoteAppPass2026!" \
        -keypass "RemoteAppPass2026!" \
        -dname "CN=Universal Remote Dev, OU=Mobile, O=OmniTech, L=Mumbai, ST=Maharashtra, C=IN"
    
    echo "✅ Keystore created successfully at: $KEYSTORE_PATH"
fi

echo "📝 Creating android/key.properties..."
cat <<EOF > "$PROPERTIES_FILE"
storePassword=RemoteAppPass2026!
keyPassword=RemoteAppPass2026!
keyAlias=upload
storeFile=upload-keystore.jks
EOF

echo "✅ key.properties generated successfully at $PROPERTIES_FILE"
echo ""
echo "🔒 IMPORTANT SECURITY NOTICE:"
echo "1. NEVER commit android/key.properties or upload-keystore.jks to public GitHub!"
echo "2. Add them to your .gitignore immediately:"
echo "   /android/key.properties"
echo "   /android/app/upload-keystore.jks"
echo ""
echo "🚀 To build your release Android App Bundle (AAB):"
echo "   flutter build appbundle --release --obfuscate --split-debug-info=./build/app/outputs/symbols"
echo "=========================================================="
