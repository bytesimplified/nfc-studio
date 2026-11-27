# NFC Studio

Native Java Android NFC tag manager and tap-based automation prototype, version 0.1. Android 10+ (API 29), NFC hardware required. No accounts, ads, or third-party runtime libraries.

## Build
Open the repository's Actions tab, select Build NFC Studio APK and run the workflow. Download NFC-Studio-APK from the successful run's artifacts and extract app-debug.apk. This is a debug-signed APK, not a Play Store release. Different CI runners may use different debug signing keys; back up profiles before reinstalling.

Local build: JDK 17, Gradle 8.9, Android SDK 35 and build-tools 35.0.0. Run gradle :app:assembleDebug.

## Status
Experimental source; Android compilation and physical NFC testing have not yet been verified. NDEF read/write/format/copy/erase/lock, local tag profiles, app launches and supported Android settings are intended features. Android permissions and hardware restrictions apply. Do not use valuable or permanently locked tags for first tests. Backups can contain private webhook URLs; keep them private.
