# Technical Release Audit

Repository root used: `C:\Users\Admin\Desktop\WhatsAppScheduler\WhatsAppScheduler`

Core Android configuration after remediation:

- Application id: `io.github.alexandrdobryn.waschedule`
- Namespace: `com.example.wascheduler`
- Version code: `6`
- Version name: `0.1.5`
- Min SDK: `31`
- Compile SDK: `36`
- Target SDK: `36`
- Release minify: enabled
- Release resource shrink: enabled
- Debug application id suffix: `.debug`
- App bundle produced: yes

Technical status:

- Local build readiness: PASS
- Play target API blocker: PASS
- Release artifact readiness: PASS
- Runtime readiness: NOT TESTED
- Production readiness: NO

Android 16 behavior-change review:

| Area | Current WA Schedule impact | Remediation result |
| --- | --- | --- |
| Background activity launch | `WakeUnlockActivity` and WhatsApp launch still require runtime validation under background restrictions. No code claim of PASS. | NOT TESTED |
| AlarmManager | Exact scheduler uses `setExactAndAllowWhileIdle`, checks `canScheduleExactAlarms()`, keeps `SCHEDULE_EXACT_ALARM`. | STATIC PASS |
| Exact alarm grant/revoke | Added manifest receiver for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` and routes to DB-backed reschedule path. | STATIC PASS, runtime NOT TESTED |
| WorkManager | Receivers enqueue one-off work; Android 16 JobScheduler quota changes require runtime reliability testing. | NOT TESTED |
| AccessibilityService | Scoped to consumer WhatsApp only, deterministic user-created rule execution. | STATIC PASS, Play review required |
| PendingIntent | Scheduler uses explicit immutable broadcast PendingIntent. | STATIC PASS |
| Notifications | Local notification channel remains; no `INTERNET`. Runtime notification permission denial still needs device test. | STATIC PASS, runtime NOT TESTED |
| Edge-to-edge | API 36 disables opt-out behavior; no dedicated UI runtime screenshot test was possible. | NOT TESTED |
| WakeLock | Partial WakeLock has explicit timeout and release path. | STATIC PASS |
| Keyguard/screen-off | Secure lock is not bypassed in code; non-secure dismiss uses Android API. | STATIC PASS, runtime NOT TESTED |

Mandatory commands run after remediation:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:lintDebug --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
.\gradlew.bat :app:bundleRelease --no-daemon
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

Result: all six commands completed with `BUILD SUCCESSFUL`.

Build caveats:

- Local SDK Platform 36 had to be installed into `.toolchain/android-sdk`.
- `local.properties` was changed locally to use that writable SDK path; this file is ignored and must not be committed.
- Gradle reports an Android SDK XML version warning and a non-fatal metrics warning for `C:\Users\Admin\.android`.
