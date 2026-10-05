# GitHub Actions CI/CD Guide for Flutter Universal TV Remote
## Automated Building of Release AAB (Google Play) & APK (Testing)

This repository includes a production-ready GitHub Actions workflow file located at:
`/.github/workflows/build_flutter_apk_aab.yml`

Whenever you push to `main` or push a version tag like `v1.0.0`, GitHub will automatically spin up an Ubuntu runner, install Flutter, configure Android SDK, compile, obfuscate, and generate both **Android App Bundle (.aab)** and **Universal APKs**.

---

### 1. Step-by-Step GitHub Secrets Setup (For Release Signing)

To ensure your APK and AAB are signed with your production upload keystore, add these 4 Secrets in your GitHub repository:

1. Open your GitHub repo in browser.
2. Go to **Settings > Secrets and variables > Actions > New repository secret**.
3. Add the following secrets:

| Secret Name | How to Get the Value | Example Value |
| :--- | :--- | :--- |
| `KEYSTORE_BASE64` | Run in terminal: `base64 -w 0 android/app/upload-keystore.jks` (or on macOS: `base64 -i android/app/upload-keystore.jks \| tr -d '\n'`) and copy the text output | `MIIEvgIBADANBgkqhkiG9w0BAQEFAASCB...` |
| `KEYSTORE_PASSWORD` | The password you entered when creating the keystore | `RemoteAppPass2026!` |
| `KEY_ALIAS` | The alias specified during keystore creation | `upload` |
| `KEY_PASSWORD` | Key password (usually same as keystore password) | `RemoteAppPass2026!` |

*(Note: If you do not configure these secrets immediately, the workflow will automatically generate a fallback CI keystore so your build never fails).*

---

### 2. How to Trigger the Build

#### Option A: Automatic on Git Push
Every time you push commits to `main` or `master`, GitHub Actions will start automatically.

#### Option B: Trigger Manually from GitHub Web UI
1. Go to the **Actions** tab in your GitHub repository.
2. In the left sidebar, click on **Build & Release Flutter TV Remote (APK & AAB)**.
3. Click the **Run workflow** dropdown button.
4. Choose build type (`all`, `aab`, or `apk`) and click **Run workflow**.

#### Option C: Trigger Release with Version Tag
To create a formal release with attached binaries:
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions will automatically build the AAB and APK, attach them to a new GitHub Release page, and generate changelog release notes.

---

### 3. Where to Download the Built Files

Once the workflow finishes (typically takes 3–5 minutes):
1. Click on the completed workflow run under the **Actions** tab.
2. Scroll down to the **Artifacts** section at the bottom:
   - `google-play-app-bundle-aab`: Contains `app-release.aab` (Upload this directly to Google Play Console!).
   - `direct-install-release-apks`: Contains `app-release.apk` (Install directly on your phone or share with beta testers!).
