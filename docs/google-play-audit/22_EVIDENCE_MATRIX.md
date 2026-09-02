# Evidence Matrix

| Area | Evidence | Result |
| --- | --- | --- |
| API level | `aapt dump badging` reports compile SDK 36 and target SDK 36 | PASS |
| Target API policy | Android 16 / API 36 required for uploads starting 2026-08-31 | PASS |
| Build | `:app:assembleDebug` | PASS |
| Unit tests | `:app:testDebugUnitTest` | PASS |
| Lint | `:app:lintDebug` | PASS with 58 warnings |
| Release APK | `:app:assembleRelease` | PASS |
| Release AAB | `:app:bundleRelease` | PASS |
| Debug androidTest compile | `:app:compileDebugAndroidTestKotlin` | PASS |
| Package identity | `io.github.alexandrdobryn.waschedule` | PASS |
| Version | `versionCode=6`, `versionName=0.1.5` | PASS |
| APK signature | `apksigner verify --verbose --print-certs` | PASS |
| AAB signature | `jarsigner -verify -certs` reports `jar verified` | PASS with warnings |
| Merged manifest exact alarm | `SCHEDULE_EXACT_ALARM` present | PASS |
| Merged manifest restricted alarm | `USE_EXACT_ALARM` absent | PASS |
| Merged manifest package visibility | only `com.whatsapp` query remains | PASS |
| Exact alarm grant receiver | `ExactAlarmPermissionReceiver` present in merged manifest | PASS |
| Accessibility package scope | `accessibility_service_config.xml` uses `com.whatsapp` | PASS |
| Accessibility flags | `flagRetrieveInteractiveWindows` removed | PASS |
| Internet permission | Not declared | PASS |
| Ads SDK | Not found in scans | PASS |
| Analytics SDK | Not found in scans | PASS |
| Landing public URL | `curl.exe -I https://alexandrdobryn-afk.github.io/WhatsAppScheduler/` | PASS HTTP 200 on 2026-09-02 |
| Privacy public URL | `curl.exe -I https://alexandrdobryn-afk.github.io/WhatsAppScheduler/privacy/` | PASS HTTP 200 on 2026-09-02 |
| Pages publish root | Workflow uploads `docs/site`; landing page, privacy page, robots.txt, and sitemap.xml now exist under `docs/site` | PASS |
| In-app Privacy Policy link | Settings -> About -> Privacy Policy opens external public URL intent | PASS |
| In-app Support contact | Settings -> About -> Support opens `mailto:alexapp.support@gmail.com` | PASS |
| Support contact | `alexapp.support@gmail.com` | PASS |
| Accessibility disclosure strings | EN/RU/UK resources updated to final declaration meaning | PASS |
| ADB device | `adb devices -l` showed no devices | NOT AVAILABLE |
| Runtime instrumentation | Requires connected emulator/device | NOT TESTED |
| WhatsApp E2E | Requires WhatsApp on emulator/device | NOT TESTED |
| Screen-off/lock | Requires emulator/device | NOT TESTED |
| Migration | Requires old install/state | NOT TESTED |
| Reliability 30 sends | Requires emulator/device | NOT TESTED |
