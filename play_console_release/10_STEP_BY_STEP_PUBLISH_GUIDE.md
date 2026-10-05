# End-to-End Google Play Console Publishing Guide
## Universal TV Remote Control (Flutter Mobile Application)

Follow this comprehensive roadmap to publish your app seamlessly on Google Play.

---

### Step 1: Prepare Your Flutter Environment & Bundle ID
1. Ensure your app namespace / package name is unique (e.g., `com.omnitech.universal.tv.remote`).
2. Run clean and get dependencies:
   ```bash
   flutter clean
   flutter pub get
   ```

---

### Step 2: Configure Signing & Build Release AAB
1. Run `./play_console_release/09_KEYSTORE_SETUP_AND_COMMANDS.sh` or create `android/key.properties` manually.
2. Build the optimized Android App Bundle (AAB):
   ```bash
   flutter build appbundle --release --obfuscate --split-debug-info=./build/app/outputs/symbols
   ```
3. Locate the output file:
   `build/app/outputs/bundle/release/app-release.aab`

---

### Step 3: Google Play Console Initial App Setup
1. Log in to [Google Play Console](https://play.google.com/console).
2. Click **Create app**:
   - **App name:** `Universal TV Remote: IR & WiFi`
   - **Default language:** `English (United States) - en-US`
   - **App or game:** `App`
   - **Free or paid:** `Free`
   - Accept the Developer Program Policies and US export laws.

---

### Step 4: Complete Policy & App Content
Under **Policy and Programs > App Content**, complete each required task:
1. **Privacy Policy:** Paste `https://your-domain.web.app/privacy-policy.html` (Use template from `02_PRIVACY_POLICY.html`).
2. **App Access:** Select "All functionality is available without special access".
3. **Ads:** Select "No" (or "Yes" if AdMob is linked).
4. **Content Ratings:** Complete IARC survey using answers in `08_APP_CONTENT_QUESTIONNAIRE.md`.
5. **Target Audience:** Select `18 and over` and `13-17`.
6. **Data Safety:** Follow the JSON mapping in `03_DATA_SAFETY_DECLARATION.json`.
7. **Government / Financial / Health:** Declare "No".
8. **Permissions Declaration:** Submit text from `07_PERMISSIONS_JUSTIFICATION.md` for BLE and Wi-Fi Multicast lock.

---

### Step 5: Set Up Store Listing & Graphics
Under **Grow > Store presence > Main store listing**:
1. Copy Title, Short Description, and Full Description from `01_STORE_LISTING_DETAILS.md`.
2. Upload Graphics:
   - **App Icon:** 512 x 512 PNG.
   - **Feature Graphic:** 1024 x 500 PNG/JPEG.
   - **Screenshots:** At least 4 phone screenshots (1080 x 2400) showcasing IR Remote mode, Smart TV Wi-Fi discovery, Indian brand selector, and D-Pad controls.

---

### Step 6: Roll Out to Internal / Closed Testing Track
1. Go to **Release > Testing > Closed testing**.
2. Click **Create new release** and upload `app-release.aab`.
3. Release notes:
   `Initial release of Universal TV Remote Control supporting IR Blaster, Bluetooth LE, and Wi-Fi Smart TVs with 50+ Global and Indian TV brands.`
4. Add your list of 20+ testers (email list or Google Group).
5. Share the opt-in URL with your testers.

---

### Step 7: Apply for Production
After 14 consecutive days of closed testing with 20 active opted-in testers:
1. Go to Play Console dashboard.
2. Click **Apply for Production**.
3. Answer Google's questionnaire about tester engagement and feedback.
4. Once approved (usually 2–4 business days), your Universal TV Remote app will be live globally on Google Play!
