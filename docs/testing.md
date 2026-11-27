# Testing

## JVM unit tests — test/java
Mirrors production packages; no Android device required. Covers action input validation, NFC duplicate detection, UID formatting, binding policy, write capacity and read-only rejection, backup round-trip and invalid profile/binding/version rejection. Run `gradle :app:testDebugUnitTest`.

## E2E and device integration — e2e/java
Instrumentation runs the real app via ActivityScenario and Espresso. Current journeys create a profile, restart the Activity, run it, inspect history, and cancel an NFC scan. Device-side NDEF codec tests check empty records and Wi-Fi MIME. Run `gradle :app:connectedDebugAndroidTest`. Emulator lacks physical NFC interaction; the tests are not a substitute for hardware acceptance.

## Physical NFC acceptance (manual, not yet verified)
Use a real Android phone with NFC enabled and spare NTAG213/215/216 tags.

| Case | Expected |
| --- | --- |
| New writable tag | Read type, UID, capacity; format if supported |
| Write and read | Text/URL round trip; verify bytes on formatted tags |
| Oversized payload | Reject with error; existing data remains |
| Copy | Source remains unchanged; destination receives NDEF records |
| Erase | Empty NDEF; previous automation binding removed |
| Automation | Write profile ID, return home, tap again, test app launch and media volume |
| Outside app | Unlock phone; tag dispatch opens NFC Studio where OS permits |
| Unregistered profile tag | Show details; do not run until registered |
| Duplicate taps | No rapid repeated execution |
| Permissions denied | DND/brightness/flashlight fail visibly; later actions proceed |
| Restore | Two taps restore supported settings |
| Backup | Export, import and check profile/bindings; invalid files rejected |
| Read-only lock | Use disposable tag; confirmation required, subsequent writes rejected |

## Gaps
No automated hardware NFC tests, no broad OEM/device matrix, no exhaustive UI or automation coverage, no release signing tests.
