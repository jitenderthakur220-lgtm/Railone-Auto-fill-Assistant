# RailOne Tatkal Assistant (Android MVP)

A simple Kotlin Android app that prepares journey details, saves passenger profiles locally, shows an approximate Tatkal opening countdown, copies passenger details to the clipboard, and opens the official RailOne app.

## Included
- Journey fields: From, To, train, date, class and quota
- Local passenger profiles: name, age, gender, berth preference
- Save/load/delete passenger profiles
- Copy passenger summary to clipboard
- Launch RailOne if installed; otherwise open its Google Play listing
- Countdown helper based on commonly published Tatkal opening times

## Important limitations
- This is a helper, not an official IRCTC/CRIS integration.
- It does not automatically fill fields inside RailOne. Android apps cannot safely assume another app exposes a supported autofill interface for these fields.
- It does not bypass CAPTCHA, OTP, Aadhaar authentication, queueing, anti-bot controls, payment confirmation, or final booking.
- Tatkal rules/timings can change. Confirm the current rule and the relevant train's booking opening date/time before relying on the countdown.
- Passenger information is stored in this app's private SharedPreferences on the device. Do not store passwords, OTPs, Aadhaar numbers, card numbers or UPI PINs.
- For production use, migrate storage to encrypted storage, add a privacy policy, accessibility review, and test on supported Android versions.

## Build
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Let Gradle sync.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Install the generated debug APK on your Android phone.

## App package
`com.example.railonetatkal`

## RailOne package launch
The helper tries to open `org.cris.aikyam`, the package listed for RailOne by Centre for Railway Information Systems. If that package is not installed, it opens the official Play Store listing:
https://play.google.com/store/apps/details?id=org.cris.aikyam


## Build APK in GitHub Actions (if no computer with Android Studio)
1. Create a GitHub repository and upload the contents of this project.
2. Open the repository's **Actions** tab.
3. Run **Build Android APK** (or push to the `main` branch).
4. When the workflow finishes, open the workflow run and download the `RailOneTatkalAssistant-debug-apk` artifact.
5. Extract the artifact ZIP to get `app-debug.apk`.

The cloud build requires internet access and a GitHub account. This environment did not have Android SDK/Gradle installed and could not reach Google's download host, so an APK could not be compiled directly here.
