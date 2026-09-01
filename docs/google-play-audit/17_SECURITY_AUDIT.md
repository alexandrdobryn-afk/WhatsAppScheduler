# Security Audit

Security posture observed:

- No `INTERNET` permission.
- No analytics/ads/crash SDKs found.
- Release minification and shrinking enabled.
- Backup disabled with `allowBackup=false`.
- Data extraction rules exclude the database from cloud backup/transfer.
- Keystore and local signing files are ignored and not tracked.
- AccessibilityService is protected by `BIND_ACCESSIBILITY_SERVICE`.
- Main launcher activity is exported as expected.
- Wake/unlock activity is not exported.
- App receivers are not exported in the source manifest.

Security risks:

1. AccessibilityService can inspect WhatsApp UI. This is the highest sensitivity surface.
2. Message text, target names, and schedule data are stored locally. Device compromise or app data extraction could expose them.
3. Diagnostic export may expose rule metadata and message preview if shared by the user.
4. Notification content must avoid leaking message text on lock screen.
5. Wake/unlock behavior must never bypass secure keyguard.
6. API 36 migration may change background execution assumptions.

Recommended mitigations:

- Keep Accessibility scope as narrow as possible.
- Remove unnecessary Accessibility flags.
- Fail closed on target mismatch, unknown WhatsApp UI state, secure lock, or disconnected service.
- Avoid full message text in logs/notifications/diagnostics where possible.
- Add a final release smoke test that inspects merged manifest, permissions, and signing.
- Consider an app-lock/passcode feature only after P0 runtime stability is complete.

Security status:

- Static security audit: CONDITIONAL PASS
- Runtime security validation: NOT TESTED
