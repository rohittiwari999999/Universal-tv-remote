# Google Play Console App Content & Policy Checklist
## Complete Step-by-Step Questionnaire Guide

Complete each section in **Google Play Console > Policy and Programs > App Content**:

---

### 1. Privacy Policy
- **URL to Enter:** `https://omnitechremotes.web.app/privacy-policy.html` *(or your hosting URL)*

---

### 2. Ads
- **Question:** Does your app contain advertisements?
- **Answer:** 
  - If you integrate Google AdMob: Select **Yes, my app contains ads**.
  - If ad-free clean utility: Select **No, my app does not contain ads**.

---

### 3. App Access
- **Question:** Is any part of your app restricted based on login credentials or memberships?
- **Answer:** Select **All functionality is available without special access restrictions**.
- *(No login or account creation is required to use the remote).*

---

### 4. Content Ratings (IARC Questionnaire)
1. **Category:** Select `Utility, Productivity, Communication or Other`.
2. **Violence:** Select `No`.
3. **Sexuality / Nudity:** Select `No`.
4. **Language / Profanity:** Select `No`.
5. **Controlled Substances:** Select `No`.
6. **Miscellaneous:**
   - Does the app natively allow users to interact or exchange content? `No`.
   - Does the app share user location with others? `No`.
   - Does the app allow digital goods purchases? (Select `No` unless in-app purchases are enabled).
- **Result:** You will receive an **Everyone (PEGI 3 / ESRB Everyone)** rating badge worldwide.

---

### 5. Target Audience & Content
- **Target Age Groups:** Select `18 and over` and `13-17`.
  *(Pro-tip: Do NOT check under 13 unless you want to comply with the Google Play Designed for Families program, which imposes strict restrictions on ad networks and APIs).*
- **Appeal to Children:** Select `No, not unintentionally appealing to children under 13`.

---

### 6. Declarations for Specialized Categories
- **News Apps:** `No, this is not a news app.`
- **COVID-19 Contact Tracing & Status:** `No, this is not a COVID-19 app.`
- **Data Safety:** Follow answers from `03_DATA_SAFETY_DECLARATION.json`.
- **Financial Features:** `My app does not provide any financial features.`
- **Health Apps:** `My app is not a health app.`
- **Government Apps:** `My app does not represent a government entity.`

---

### 7. Google Play 2024–2026 Testing Requirement (Mandatory for Personal Accounts)
If your Google Play Developer account was created after November 13, 2023:
1. Google requires you to run a **Closed Testing Track** with **at least 20 opted-in testers**.
2. Testers must remain enrolled for **at least 14 consecutive days**.
3. **Recommended approach:**
   - Invite friends, family, or use a closed testing group (such as Reddit r/AndroidClosedTesting or dedicated QA tester groups).
   - Testers install the app bundle from the Play Store closed test link.
   - After 14 days, click **Apply for Production** in Play Console.
