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

## Newly Confirmed P0 Submission Blocker

1. User-visible Privacy Policy link is not present in the app.
   - Checked: `SettingsScreen.kt` and app resources.
   - Current result: no Settings/About Privacy Policy link found.
   - Required action: add a user-visible Privacy Policy link in a separate Android/UI change, for example Settings -> About -> Privacy Policy.
   - Status: BLOCKING PLAY SUBMISSION.

## Remaining Non-Code Submission Prerequisites

1. Provide support email or stable support page.
2. Record Accessibility review video.
3. Complete Play Console forms.
4. Enable GitHub Pages Source = GitHub Actions, rerun the Pages workflow, and verify public landing and Privacy Policy URLs after deployment.

## Remaining Runtime Risks

1. Background activity launch behavior for `WakeUnlockActivity`/WhatsApp launch on API 36 is not runtime-tested.
2. WorkManager/JobScheduler quota behavior on Android 16 is not reliability-tested.
3. Exact alarm grant/deny/revoke behavior is not runtime-tested.
4. Screen-off, non-secure keyguard, and secure lock flows are not runtime-tested.
5. 30-send reliability test is not complete.

## Recommended Next Order

1. Add in-app Privacy Policy link.
2. User provides support email.
3. Enable GitHub Pages Source = GitHub Actions in repository settings.
4. Rerun the Pages workflow and verify `/` and `/privacy/`.
5. Install release APK/AAB-derived build on Pixel 8 / Android 15 and Android 16 emulator/device.
6. Execute `23_MANUAL_RUNTIME_VALIDATION_CHECKLIST.md`.
7. Record Accessibility review video from the same behavior.
8. Upload AAB to internal testing.
9. Start closed testing and production access process if required.
