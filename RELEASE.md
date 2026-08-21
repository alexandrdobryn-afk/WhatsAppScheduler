# Private release

This app uses AccessibilityService for local WhatsApp UI automation. Do not publish it to Google Play unless the policy question is resolved separately. For personal/private use, distribute a signed release APK through a GitHub Release, direct repository download, private cloud link, USB, or another trusted channel.

## Signing files

Release signing is configured from either local `keystore.properties` or environment variables:

```properties
WASCHEDULER_STORE_FILE=.release/whatsapp-scheduler-release.jks
WASCHEDULER_STORE_PASSWORD=...
WASCHEDULER_KEY_ALIAS=whatsapp-scheduler-release
WASCHEDULER_KEY_PASSWORD=...
```

These files are ignored by Git:

- `.release/`
- `keystore.properties`
- `*.jks`
- `*.keystore`
- `*.p12`

Create the local key once:

```powershell
.\scripts\create-release-keystore.ps1
```

For non-interactive local setup:

```powershell
.\scripts\create-release-keystore.ps1 -GenerateRandomPasswords
```

Back up `.release\whatsapp-scheduler-release.jks` and `keystore.properties` somewhere private. Losing the key prevents normal updates over an installed release. Leaking the key allows someone else to sign an update with the same identity.

## Build

```powershell
.\gradlew.bat :app:assembleRelease
```

Output:

```text
app\build\outputs\apk\release\app-release.apk
```

## Verify signature

```powershell
.\.toolchain\android-sdk\build-tools\35.0.0\apksigner.bat verify --print-certs app\build\outputs\apk\release\app-release.apk
```

## GitHub distribution

1. Keep the repository public or private as desired, but never commit `.release/` or `keystore.properties`.
2. Build `app-release.apk`.
3. Copy the signed APK to `download/app-release.apk` if GitHub Releases are not available.
4. Commit and push `download/app-release.apk`.
5. Install the APK on the phone from:

```text
https://raw.githubusercontent.com/alexandrdobryn-afk/WhatsAppScheduler/main/download/app-release.apk
```

Before releasing an update, bump `versionCode` in `app/build.gradle.kts` and sign with the same keystore. Android will then allow installing the new APK over the old one while preserving Room data.
