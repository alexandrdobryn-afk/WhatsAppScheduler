# Play Console Checklist

Before internal testing upload:

- [x] Upgrade target SDK to API 36.
- [x] Remove `USE_EXACT_ALARM`.
- [x] Keep `SCHEDULE_EXACT_ALARM` with user grant flow.
- [x] Rebuild release APK.
- [x] Rebuild release AAB.
- [x] Verify package id, versionCode, versionName, targetSdk, compileSdk.
- [x] Verify APK signature.
- [x] Verify AAB JAR signature.
- [x] Narrow Accessibility scope to `com.whatsapp`.
- [x] Remove `flagRetrieveInteractiveWindows`.
- [x] Prepare public Privacy Policy URL: `https://alexandrdobryn-afk.github.io/WhatsAppScheduler/privacy/`.
- [x] Correct GitHub Pages publish structure under `docs/site`.
- [x] Add a user-visible in-app Privacy Policy link.
- [x] Add a user-visible Support contact.
- [x] Replace support placeholder with `alexapp.support@gmail.com`.
- [x] Verify `/` and `/privacy/` return HTTP 200.
- [ ] Record Accessibility review video.
- [ ] Complete App Content forms.
- [ ] Complete Data Safety form.
- [ ] Complete Accessibility API declaration.
- [ ] Add store screenshots from the validated release build.
- [ ] Add feature graphic and final app icon.

Before closed testing:

- [ ] Pass internal install/update smoke test.
- [ ] Pass onboarding and disclosure flow.
- [ ] Pass Accessibility enable/disable flow.
- [ ] Pass exact-alarm denied/granted/revoked scenarios.
- [ ] Pass schedule type matrix.
- [ ] Pass WhatsApp target verification matrix.
- [ ] Pass background/removal/screen-off scenarios.
- [ ] Pass v2 to current migration.
- [ ] Run at least 30 scheduled sends and record results.

Before production:

- [ ] Complete 12-tester / 14-day closed test if applicable.
- [ ] Review crashes, ANRs, vitals, and policy warnings.
- [ ] Confirm no new permissions or SDKs were introduced.
- [ ] Confirm Data Safety still matches binary behavior.
- [ ] Confirm support path works.
