# Blockers and Action Plan

## Resolved P0 Code Blockers

1. Target SDK below current Play requirement
   - Before: `compileSdk = 35`, `targetSdk = 35`.
   - After: `compileSdk = 36`, `targetSdk = 36`.
   - Status: RESOLVED.

2. Restricted exact alarm permission
   - Before: manifest declared both `USE_EXACT_ALARM` and `SCHEDULE_EXACT_ALARM`.
   - After: `USE_EXACT_ALARM` removed; `SCHEDULE_EXACT_ALARM` retained.
   - Status: RESOLVED.

3. Unsupported WhatsApp Business scope
   - Before: manifest, query scope, Accessibility config, and code allowed `com.whatsapp.w4b`.
   - After: scope narrowed to `com.whatsapp`.
   - Status: RESOLVED.

4. Unneeded Accessibility window enumeration flag
   - Before: `flagRetrieveInteractiveWindows`.
   - After: removed; `rootInActiveWindow` path remains.
   - Status: RESOLVED.

## Remaining Non-Code Submission Prerequisites

1. Provide support email or stable support page.
2. Record Accessibility review video.
3. Complete Play Console forms.
4. Verify public Privacy Policy URL after GitHub Pages deployment.

## Remaining Runtime Risks

1. Background activity launch behavior for `WakeUnlockActivity`/WhatsApp launch on API 36 is not runtime-tested.
2. WorkManager/JobScheduler quota behavior on Android 16 is not reliability-tested.
3. Exact alarm grant/deny/revoke behavior is not runtime-tested.
4. Screen-off, non-secure keyguard, and secure lock flows are not runtime-tested.
5. 30-send reliability test is not complete.

## Recommended Next Order

1. User provides support email.
2. Install release APK/AAB-derived build on Pixel 8 / Android 15 and Android 16 emulator/device.
3. Execute `23_MANUAL_RUNTIME_VALIDATION_CHECKLIST.md`.
4. Record Accessibility review video from the same behavior.
5. Upload AAB to internal testing.
6. Start closed testing and production access process if required.
