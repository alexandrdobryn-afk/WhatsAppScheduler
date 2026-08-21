# WA Schedule

**Schedule WhatsApp messages automatically on Android using your personal WhatsApp account.**

WA Schedule is an open-source Android WhatsApp scheduler for personal accounts.
Set a chat or group, message text, weekdays, start date, and exact send times.
The app runs locally on your phone and uses Android AccessibilityService to
interact with the official WhatsApp application.

**No WhatsApp Business API · No server · No root · Local-first**

[Download latest APK](https://github.com/alexandrdobryn-afk/WhatsAppScheduler/releases/latest/download/app-release.apk)

Current Android package name:

```text
io.github.alexandrdobryn.waschedule
```

## Test Status

Tested on:

| Area | Status |
|---|---|
| Android Emulator | Pixel 8, Android 15 / API 35 reported as tested |
| APK installation | Passed |
| Accessibility onboarding | Passed |
| WhatsApp installation and login | Passed |
| Rule editor | Under active testing |
| Scheduled end-to-end send | Experimental / not yet fully validated |

Build verification in this repository:

| Check | Status |
|---|---|
| `:app:assembleDebug` | Passed |
| `:app:assembleRelease` | Passed |
| `:app:testDebugUnitTest` | 29 passed, 0 failed, 0 skipped |
| `:app:lintDebug` | Passed, 0 errors, 53 warnings |
| `:app:lintRelease` | Passed, 0 errors, 53 warnings |
| Release APK signature | Verified with `apksigner` |

## Screenshots

Product screenshots are being collected for the public README. The intended
set is:

| Home | Schedule | History | Settings |
|---|---|---|---|
| Coming soon | Coming soon | Coming soon | Coming soon |

## Features

- Schedule WhatsApp messages for personal WhatsApp accounts.
- Send to a configured WhatsApp chat or group.
- Choose a start date, weekdays, and one or more exact send times.
- Run locally on Android with no backend server.
- Uses Android `AccessibilityService` to operate the official WhatsApp UI.
- Stores only user-created rules and execution history in a local Room database.
- Supports English, Ukrainian, and Russian interface text.
- Includes diagnostics for WhatsApp, Accessibility, notifications, exact alarms,
  battery restrictions, scheduler time zone, and last execution status.

## Install

1. Open the APK link on the Android phone:
   [download/app-release.apk](https://github.com/alexandrdobryn-afk/WhatsAppScheduler/releases/latest/download/app-release.apk).
2. Android will ask whether to allow installing unknown apps from the browser or
   file manager you used. Allow it for that source.
3. Install WA Schedule.
4. Open the app and complete onboarding.
5. Go to Android Settings -> Accessibility -> Installed apps and enable
   `WA Schedule - automation`.

Current APK SHA-256:

```text
2FD9B044632B16538DEE30134E8668E87D7B259D6D01EA0DD3EC85CE4F037DDD
```

Do not use `app-debug.apk` for normal installation. Use the signed release APK.
Future updates must be signed with the same private keystore.

## Permissions

| Permission | Why it is needed |
|---|---|
| Accessibility Service | Required to interact with the official WhatsApp UI because personal WhatsApp has no public send-message API |
| Notifications | Shows send success/failure status |
| Exact alarms (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`) | Runs schedules at configured exact times |
| Boot completed | Restores scheduled alarms after reboot or app update |
| Wake lock / foreground service | Helps complete scheduled work reliably |
| Battery optimization exemption | Optional but recommended on aggressive OEM Android builds |

The app does not request `INTERNET`.

## Privacy And Security

- No server.
- No WhatsApp Business API.
- No root.
- No telemetry.
- No `INTERNET` permission.
- The Accessibility service is restricted to `com.whatsapp` and `com.whatsapp.w4b`.
- The app does not store other people's WhatsApp messages.
- Release builds use R8, resource shrinking, and a local signing key.
- Internal receivers and services are not exported unless Android requires it.

## Usage

1. Open WA Schedule.
2. Tap Add schedule.
3. Enter the WhatsApp chat or group name exactly as it appears in WhatsApp.
4. Enter the message text.
5. Select the start date.
6. Add one or more send times.
7. Select weekdays.
8. Save the rule.

For manual validation, use Dry Run before sending. Dry Run opens WhatsApp,
searches the chat, verifies the input field and send button, then stops before
tapping Send.

## Limitations

- Personal WhatsApp has no official public API for scheduled sending. WA Schedule
  uses Android Accessibility automation, so WhatsApp UI changes can require app
  updates.
- The app can confirm that it tapped Send in the WhatsApp UI, but it cannot
  guarantee server delivery, recipient receipt, or read status.
- If multiple chats have the same name, the app treats the target as ambiguous
  and does not send.
- Locked devices and aggressive OEM background restrictions can prevent scheduled
  work from running.
- Automated WhatsApp UI use may conflict with WhatsApp terms or app-store
  policies. Use responsibly for personal/local automation.
- Google Play distribution is not the current target because automated
  Accessibility use can conflict with store policy.

## Search Terms

WA Schedule can also be described as:

- WhatsApp message scheduler for Android
- Schedule WhatsApp messages
- Automatic WhatsApp group messages
- WhatsApp scheduled sender
- WhatsApp automation for personal account
- Android WhatsApp scheduler
- Scheduled WhatsApp group messages
- Personal WhatsApp scheduling app
- Android scheduled message sender

## Русский

WA Schedule — локальный Android-планировщик для автоматической отправки заранее
подготовленных сообщений в WhatsApp от имени владельца телефона.

Приложение не использует сервер, WhatsApp Business API или root. Оно хранит
только созданные пользователем правила и использует Android AccessibilityService
для взаимодействия с официальным приложением WhatsApp.

## For Developers

- Android Gradle Plugin: 8.5.2
- Kotlin: 1.9.24
- JDK: 17
- minSdk: 31
- targetSdk / compileSdk: 35
- UI: Jetpack Compose
- DI: Hilt
- Storage: Room + DataStore
- Scheduling: AlarmManager + WorkManager

Build:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:lintRelease
```

Architecture details are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
Private release signing details are in [RELEASE.md](RELEASE.md).
