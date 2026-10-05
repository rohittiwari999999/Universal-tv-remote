# Google Play Console Permissions Justification Declarations
## Required Declarations for Review Approval

When submitting your app in Google Play Console under **Policy > App Content > Permissions & Declarations**, copy and paste the following approved justification texts.

---

### 1. Bluetooth Low Energy (BLE) Declaration: `BLUETOOTH_SCAN` & `BLUETOOTH_CONNECT`

#### Form Question: "Does your app use Bluetooth scanning to derive physical location?"
- **Select:** `No`
- **Confirmation:** Ensure your `AndroidManifest.xml` includes:
  `android:usesPermissionFlags="neverForLocation"`

#### Declaration Justification Text (Copy & Paste):
```text
The Universal TV Remote Control app uses Bluetooth Low Energy (BLE) scanning exclusively to discover nearby Bluetooth-enabled Smart TVs and TV streaming set-top boxes in the immediate room. Once paired, the app sends standard remote navigation commands (Power, Directional D-Pad, Volume, and Channel changes) via BLE GATT characteristics. 

The app does not collect, record, or track user physical location, nor does it log beacon data or GPS coordinates. The neverForLocation flag is explicitly declared in our AndroidManifest.xml.
```

---

### 2. Wi-Fi Multicast Lock: `CHANGE_WIFI_MULTICAST_STATE`

#### Declaration Justification Text (Copy & Paste):
```text
Our application requires the CHANGE_WIFI_MULTICAST_STATE permission to send UDP multicast discovery packets (SSDP M-SEARCH protocol on 239.255.255.250:1900 and mDNS Bonjour queries) over the user's local home Wi-Fi network. 

This enables the app to automatically detect Smart TV operating systems connected to the same LAN (including Samsung Tizen OS, LG webOS, Android TV/Google TV, and Sony Bravia). This discovery is initiated strictly upon user request when searching for Smart TVs. No Wi-Fi network credentials, external IPs, or routing tables are extracted or transmitted outside the local device.
```

---

### 3. Consumer Infrared Hardware: `android.hardware.consumerir`

#### Declaration Justification Text (Copy & Paste):
```text
The app includes functionality to control non-smart traditional TVs using carrier-frequency infrared signals (TRANSMIT_IR). In accordance with Google Play quality guidelines, this hardware feature is explicitly declared with android:required="false" in our AndroidManifest.xml. This ensures users whose smartphones lack an IR blaster (such as modern flagships and iPhones) can still discover and use the application via Bluetooth and Wi-Fi Smart TV protocols without experiencing installation blockers or device-filtering rejections.
```

---

### 4. Background Location Declaration:
- **Select:** `No background location accessed.`
*(Our app does not request ACCESS_BACKGROUND_LOCATION, ACCESS_FINE_LOCATION, or ACCESS_COARSE_LOCATION).*
