# NFC Studio v0.3

Native Android NFC tag management and tap-triggered automations. Android 10+; NFC hardware is required for tag operations. No account, advertisements or root. This is a prototype, not a Play Store release.

## Architecture

```
app/src/main/java/com/nfcstudio/app/
├── MainActivity.java       # Android entry point and lifecycle
├── automation/             # Validation, sequential execution, Android actions, HTTPS
├── data/                   # Local profile repository, JSON and backup validation
├── nfc/                    # Tag I/O, record encoding, write and trigger policies
└── ui/                     # Shared UI and separate home, tag, automation, settings screens
test/java/com/nfcstudio/app/ # JVM unit tests (mirrors production packages)
e2e/java/com/nfcstudio/app/  # Android instrumentation and UI journeys
docs/                       # Test matrix and device acceptance
```

Java 17, Android SDK 35, Gradle 8.9 and AGP 8.7.3. Java is retained to minimize migration risk; Gradle uses Groovy and CI uses YAML/shell. Tests use JUnit and Espresso. The UI uses a custom Material-inspired native view design system; storage is still SharedPreferences/JSON. Compose, Room, MVVM and per-action handlers are future work.

## Build and test

GitHub Actions runs JVM unit tests, compiles the app APK, verifies the debug signature and uploads the APK. A separate E2E job compiles the instrumented test APK and runs tests on an Android emulator. Download **NFC-Studio-APK** from a successful APK job. E2E results are separate. Different CI runners can use different debug keys; back up profiles before reinstalling.

Local commands (JDK 17, Gradle 8.9, SDK 35/build-tools 35.0.0):
```sh
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
gradle :app:connectedDebugAndroidTest # emulator or USB-connected phone
```

## Features

- Inspect NDEF tags, UID, technologies, capacity, writability; write, format, erase, copy records and lock compatible tags (irreversible).
- Standard NDEF text, URI, phone, email, SMS, GPS, vCard, Wi-Fi WSC and Android app records. Wi-Fi behavior varies by OS. Blank format operations need a follow-up verification scan.
- Local multi-action profiles, app chooser, links, volume/ringer, DND, brightness, rotation, flashlight, vibration, message, alarm/timer, Wi-Fi/Bluetooth settings panels, HTTPS POST, delay; permission restrictions apply.
- Register profile tags, duplicate prevention, confirmation, manual run, restore supported settings, history and JSON import/export. Tag IDs are not cryptographic authentication. Do not use NFC as a security credential.
- Exact chip ID, password protection, conditional actions and raw-memory editing are not implemented.

## v0.3 visual refresh
- AMOLED black, midnight blue, light and system themes.
- Gradient dashboard, vector icons, animated NFC radar (respects system animation settings), fixed bottom navigation, live profile/tag counts.
- Record-type grid and live payload byte preview, structured scan result cards.
- Routine cards, enabled switches, editable templates, action reordering, installed app picker with icons.
- Permission dashboard and local backup/history screens.
- No sample tags or fake NFC success; scanning uses real hardware.

## Hardware and privacy

Emulator tests do not prove physical NFC read/write or background launching. See [hardware acceptance checklist](docs/testing.md). Use a spare writable tag first. Backups may contain private webhook URLs; keep them private. Data is stored on the device. HTTPS requests occur only for configured webhooks.

## Limitations

The new UI is designed for modern large Android displays but has not yet been verified on a Galaxy S25 Ultra. Some actions depend on device-specific behavior, permissions and available apps. Automation running when an Activity is destroyed is cancelled rather than silently continued in the background.
