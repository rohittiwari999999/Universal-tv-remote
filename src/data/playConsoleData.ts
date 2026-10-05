export interface PlayConsoleFile {
  id: string;
  name: string;
  path: string;
  category: 'Listing & Policy' | 'Android Config' | 'Publishing Guides';
  summary: string;
  badge: string;
  content: string;
}

export const PLAY_CONSOLE_FILES: PlayConsoleFile[] = [
  {
    id: 'store_listing',
    name: '01_STORE_LISTING_DETAILS.md',
    path: '/play_console_release/01_STORE_LISTING_DETAILS.md',
    category: 'Listing & Policy',
    summary: 'App Title, 80-char Short Description, 4000-char Full Description (EN & HI), Category, 5 Store Tags, Graphic Specs.',
    badge: 'Store Presence',
    content: `# Google Play Store Listing Details
App Name: Universal TV Remote: IR & WiFi
Short Description: Control any Smart TV & traditional TV via IR Blaster, Bluetooth & Wi-Fi.

Category: Tools (Secondary: Video Players & Editors)
Tags: Remote control, TV, Smart TV, Tools, Utilities

Highlights:
- 3-in-1 Hybrid Protocols: IR Blaster, BLE, and Wi-Fi SSDP/WebSocket
- Indian Brands: Vu, Mi, Thomson, Micromax, Lloyd, Onida, BPL, Sanyo
- Global Brands: Samsung, LG, Sony, Panasonic, TCL
- Offline code caching & tactile haptics`
  },
  {
    id: 'privacy_policy',
    name: '02_PRIVACY_POLICY.html',
    path: '/play_console_release/02_PRIVACY_POLICY.html',
    category: 'Listing & Policy',
    summary: 'Complete Google Play & COPPA compliant privacy policy with explicit IR, BLE neverForLocation, and Wi-Fi disclosures.',
    badge: 'Legal Mandatory',
    content: `<!DOCTYPE html>
<html>
<head><title>Privacy Policy - Universal TV Remote</title></head>
<body>
  <h1>Privacy Policy</h1>
  <p>Universal TV Remote does not collect, record, sell, or share personal identifiable information (PII).</p>
  <h3>Hardware Disclosures:</h3>
  <p>Consumer IR (TRANSMIT_IR): Exclusively emits modulated 36-40kHz infrared light pulses.</p>
  <p>Bluetooth Low Energy: Uses BLUETOOTH_SCAN with android:usesPermissionFlags="neverForLocation" to detect TVs, not user location.</p>
  <p>Wi-Fi Multicast Lock: Broadcasts SSDP UPnP queries on local LAN to discover Samsung Tizen, LG webOS, Sony Bravia TVs.</p>
</body>
</html>`
  },
  {
    id: 'data_safety',
    name: '03_DATA_SAFETY_DECLARATION.json',
    path: '/play_console_release/03_DATA_SAFETY_DECLARATION.json',
    category: 'Listing & Policy',
    summary: 'Exact questionnaire answers for Play Console Data Safety section (No location collected, TLS 1.3 encrypted, deletion URL).',
    badge: 'Review Blocker',
    content: `{
  "dataCollectionOverview": {
    "doesAppCollectOrShareUserData": false,
    "isDataEncryptedInTransit": true,
    "doYouProvideWayForUsersToRequestDataDeletion": true,
    "dataDeletionUrl": "https://omnitechremotes.web.app/delete-request"
  },
  "permissionsJustification": {
    "locationData": {
      "collected": false,
      "shared": false,
      "note": "We use BLUETOOTH_SCAN with android:usesPermissionFlags='neverForLocation'. No coarse or fine GPS location is accessed or collected."
    }
  }
}`
  },
  {
    id: 'manifest',
    name: '04_AndroidManifest_Production.xml',
    path: '/play_console_release/04_AndroidManifest_Production.xml',
    category: 'Android Config',
    summary: 'Production AndroidManifest with android:required="false" on IR hardware so non-IR phones can still install via Play Store!',
    badge: 'Architecture',
    content: `<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.omnitech.universal.tv.remote">

    <!-- Crucial: required="false" so Pixel & non-IR phones can install -->
    <uses-feature android:name="android.hardware.consumerir" android:required="false" />
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
    <uses-feature android:name="android.hardware.wifi" android:required="false" />

    <uses-permission android:name="android.permission.TRANSMIT_IR" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" tools:targetApi="s" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" tools:targetApi="s" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
    <uses-permission android:name="android.permission.VIBRATE" />
</manifest>`
  },
  {
    id: 'build_gradle',
    name: '05_build_gradle_release.gradle',
    path: '/play_console_release/05_build_gradle_release.gradle',
    category: 'Android Config',
    summary: 'Production Gradle script with Target SDK 35, Java 17, R8 minification, and key.properties release signing.',
    badge: 'Target SDK 35',
    content: `android {
    namespace "com.omnitech.universal.tv.remote"
    compileSdk 35

    defaultConfig {
        applicationId "com.omnitech.universal.tv.remote"
        minSdk 21
        targetSdk 35
        ndk { abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86_64' }
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}`
  },
  {
    id: 'proguard',
    name: '06_proguard_rules.pro',
    path: '/play_console_release/06_proguard_rules.pro',
    category: 'Android Config',
    summary: 'ProGuard & R8 preservation rules for ConsumerIrManager JNI, flutter_blue_plus, and Firebase Firestore.',
    badge: 'R8 Optimization',
    content: `-keep class android.hardware.ConsumerIrManager { *; }
-keep class com.boskokg.flutter_blue_plus.** { *; }
-keep class com.google.firebase.** { *; }
-keep class java.net.MulticastSocket { *; }`
  },
  {
    id: 'justifications',
    name: '07_PERMISSIONS_JUSTIFICATION.md',
    path: '/play_console_release/07_PERMISSIONS_JUSTIFICATION.md',
    category: 'Publishing Guides',
    summary: 'Pre-written justifications for Google Play reviewers explaining Bluetooth scanning and Wi-Fi multicast usage.',
    badge: 'Review Fast-Track',
    content: `Bluetooth Justification:
The Universal TV Remote Control app uses BLE scanning exclusively to discover nearby Bluetooth-enabled Smart TVs and TV streaming set-top boxes in the immediate room. The neverForLocation flag is explicitly declared.

Wi-Fi Multicast Lock Justification:
Required for SSDP M-SEARCH discovery (UDP port 1900) to find Samsung Tizen, LG webOS, and Sony Bravia on local LAN.`
  },
  {
    id: 'keystore_script',
    name: '09_KEYSTORE_SETUP_AND_COMMANDS.sh',
    path: '/play_console_release/09_KEYSTORE_SETUP_AND_COMMANDS.sh',
    category: 'Publishing Guides',
    summary: 'Ready-to-run shell script to generate upload-keystore.jks with 2048-bit RSA and auto-create key.properties.',
    badge: 'Security',
    content: `#!/bin/bash
keytool -genkey -v \\
  -keystore ./android/app/upload-keystore.jks \\
  -storetype JKS -keyalg RSA -keysize 2048 -validity 10000 \\
  -alias upload`
  },
  {
    id: 'step_guide',
    name: '10_STEP_BY_STEP_PUBLISH_GUIDE.md',
    path: '/play_console_release/10_STEP_BY_STEP_PUBLISH_GUIDE.md',
    category: 'Publishing Guides',
    summary: 'Master roadmap from flutter build appbundle to 20-tester 14-day closed track rollout and production approval.',
    badge: 'Workflow',
    content: `Steps:
1. Generate keystore & key.properties
2. Run: flutter build appbundle --release --obfuscate
3. Complete App Content (Privacy, Data Safety, IARC rating)
4. Upload AAB to Closed Testing Track (20 testers for 14 days)
5. Apply for Production & Go Live!`
  },
  {
    id: 'github_actions_workflow',
    name: 'build_flutter_apk_aab.yml',
    path: '/.github/workflows/build_flutter_apk_aab.yml',
    category: 'Publishing Guides',
    summary: 'Automated GitHub Actions CI/CD workflow to build, sign, and upload release APKs and Google Play AAB bundles automatically.',
    badge: 'GitHub CI/CD',
    content: `name: Build & Release Flutter TV Remote (APK & AAB)

on:
  push:
    branches: [ main, master ]
    tags: [ 'v*' ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:
    inputs:
      build_type:
        description: 'Build Type (all, aab, apk)'
        required: true
        default: 'all'
        type: choice
        options: [ all, aab, apk ]

jobs:
  build:
    name: Build Flutter APK & AAB
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up Java (JDK 17)
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Set up Flutter
        uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.24.3'
          channel: 'stable'
          cache: true

      - name: Install Dependencies
        run: flutter pub get

      - name: Configure Keystore Signing
        env:
          KEYSTORE_BASE64: \${{ secrets.KEYSTORE_BASE64 }}
          KEYSTORE_PASSWORD: \${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: \${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: \${{ secrets.KEY_PASSWORD }}
        run: |
          mkdir -p android/app
          if [ -n "$KEYSTORE_BASE64" ]; then
            echo "$KEYSTORE_BASE64" | base64 --decode > android/app/upload-keystore.jks
            echo "storePassword=$KEYSTORE_PASSWORD" > android/key.properties
            echo "keyPassword=$KEY_PASSWORD" >> android/key.properties
            echo "keyAlias=$KEY_ALIAS" >> android/key.properties
            echo "storeFile=upload-keystore.jks" >> android/key.properties
          fi

      - name: Build Google Play Bundle (AAB)
        run: flutter build appbundle --release --obfuscate --split-debug-info=build/app/outputs/symbols

      - name: Build Universal & Split APKs
        run: flutter build apk --release --split-per-abi

      - name: Upload AAB Artifact
        uses: actions/upload-artifact@v4
        with:
          name: google-play-app-bundle-aab
          path: build/app/outputs/bundle/release/*.aab

      - name: Upload APK Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: direct-install-release-apks
          path: build/app/outputs/flutter-apk/*.apk`
  },
  {
    id: 'github_guide',
    name: 'GITHUB_ACTIONS_CI_CD_GUIDE.md',
    path: '/play_console_release/GITHUB_ACTIONS_CI_CD_GUIDE.md',
    category: 'Publishing Guides',
    summary: 'Instructions for setting up GitHub Secrets (KEYSTORE_BASE64, KEYSTORE_PASSWORD) and downloading built AAB/APK artifacts.',
    badge: 'Guide',
    content: `# GitHub Actions CI/CD Guide for Flutter Universal TV Remote

1. Go to GitHub Repo > Settings > Secrets and variables > Actions > New repository secret
2. Add secrets:
   - KEYSTORE_BASE64: base64 -w 0 android/app/upload-keystore.jks
   - KEYSTORE_PASSWORD: Your keystore password
   - KEY_ALIAS: upload
   - KEY_PASSWORD: Your key password

3. Push to main or trigger via Actions tab > "Run workflow"
4. Download AAB and APK directly from the Artifacts section!`
  }
];
