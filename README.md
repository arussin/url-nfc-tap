# URL NFC Tap

URL NFC Tap is a tiny Android app that turns a phone into an NFC Forum Type 4 tag. Opening the app immediately begins sharing a configurable primary web link. One tap switches to a configurable secondary link.

The app uses Android's official Host Card Emulation API. It does not require root, an NFC sticker, an account, analytics, or internet access.

## Features

- Immediately emulates the primary URL when freshly opened
- One-tap switching between two labeled destinations
- Standard NDEF URI record for broad reader compatibility
- Foreground NFC preference while the app is visible
- No runtime permissions and no `INTERNET` permission
- Personal URLs kept out of source control

## Configure locally

Copy the example configuration and edit the ignored copy:

```bash
cp links.properties.example links.properties
```

```properties
nfc.primary.label=Primary link
nfc.primary.url=https://example.com/profile
nfc.secondary.label=Secondary link
nfc.secondary.url=https://example.com
```

`links.properties` is listed in `.gitignore`. The same values may be supplied at build time through these environment variables:

- `NFC_PRIMARY_LABEL`
- `NFC_PRIMARY_URL`
- `NFC_SECONDARY_LABEL`
- `NFC_SECONDARY_URL`

Environment variables take precedence over `links.properties`. When neither is present, harmless example values are used so forks and pull requests remain buildable.

## Build

Open the project in a current Android Studio release and select **Build → Build APK(s)**, or run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Build a personalized APK with GitHub Actions

Add the four environment-variable names above as GitHub Actions repository secrets, then manually run the **Android build** workflow. Manual runs require all four secrets and upload the resulting APK as the `url-nfc-tap-apk` artifact. Ordinary pushes and pull requests use example values and do not publish an APK.

## How it works

The foreground activity asks Android to prefer this app's `HostApduService`. The service implements the NFC Forum Type 4 Tag application, exposes a read-only Capability Container and NDEF file, and builds an NDEF URI record from the currently selected destination.

Closing or backgrounding the activity releases its foreground preference and makes the service decline NFC reads. The device must support Host Card Emulation, NFC must be enabled, and the sharing screen should remain open and unlocked during a tap.

## Privacy and security

Ignoring `links.properties` keeps personal values out of Git history, but it does not make transmitted links secret. URLs are embedded in the compiled APK and sent to nearby NFC readers while the app is active. Use this only for destinations you intend to share publicly.

## License

MIT
