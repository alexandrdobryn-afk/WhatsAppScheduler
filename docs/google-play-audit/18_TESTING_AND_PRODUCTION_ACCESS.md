# Testing and Production Access

Automated checks run after remediation:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:lintDebug --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
.\gradlew.bat :app:bundleRelease --no-daemon
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

Result:

- `assembleDebug`: PASS
- `testDebugUnitTest`: PASS
- `lintDebug`: PASS with 58 warnings
- `assembleRelease`: PASS
- `bundleRelease`: PASS
- `compileDebugAndroidTestKotlin`: PASS

ADB/device status:

- `adb devices -l`: no devices attached.

Runtime status:

| Scenario | Status |
| --- | --- |
| Clean install launch | NOT TESTED |
| Onboarding | NOT TESTED |
| Accessibility enable | NOT TESTED |
| Weekly Sunday | NOT TESTED |
| Weekly weekends | NOT TESTED |
| Weekly custom days | NOT TESTED |
| Multiple times | NOT TESTED |
| Specific date execution | NOT TESTED |
| Multiple dates next occurrence | NOT TESTED |
| Language System/RU/UK/EN switching | NOT TESTED |
| WhatsApp closed/open/target open/other chat open | NOT TESTED |
| WA Schedule foreground/background/destroyed/removed from recents | NOT TESTED |
| Screen off non-secure lock | NOT TESTED |
| Secure lock retry | NOT TESTED |
| v2 to current migration | NOT TESTED |
| 30 scheduled sends | NOT TESTED |

Production access note:

For personal Play developer accounts created after 2023-11-13, Google requires a closed test with at least 12 testers continuously opted in for at least 14 days before applying for production access.
