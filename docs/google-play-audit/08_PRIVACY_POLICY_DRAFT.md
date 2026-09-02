# Privacy Policy Draft

Last updated: 2026-09-02

Public policy URL: https://alexandrdobryn-afk.github.io/WhatsAppScheduler/privacy/

Support contact: alexapp.support@gmail.com

WA Schedule is a local-first Android app for scheduling user-created WhatsApp messages.

## Data Stored On Your Device

WA Schedule stores the following data locally on your device:

- Schedule/rule names.
- WhatsApp chat or group target names entered by you.
- Message text entered by you.
- Schedule dates, days, times, timezone, retry window, and rule state.
- Local execution history and status.
- App preferences such as language, onboarding state, and settings.

Rule, message, schedule, and execution-history data is stored in the app's private Room database. App preferences are stored locally with DataStore.

## Accessibility Permission

WA Schedule uses Android Accessibility permission only to perform WhatsApp actions that you configure. When a rule runs, the app may inspect the currently visible official WhatsApp screen to verify the target chat or group, find the message input field and send button, enter your saved message, tap Send, and return to Home.

WA Schedule does not intentionally read your conversation history for developer use. It does not store WhatsApp conversation history or Accessibility node trees.

## User-Directed Transfer To WhatsApp

When you run Test Send or a scheduled send, WA Schedule transfers the message text you created into WhatsApp so WhatsApp can send it to your selected chat or group. This is the core feature requested by the user-created rule or explicit test-send action.

## Diagnostics

If you create a diagnostic report, the report may include app version, Android/device metadata, permission state, Accessibility connection state, screen/keyguard state, rule count, recent execution timestamps, statuses, and structured error codes. Diagnostic reports are created only by user action and remain local unless you choose to share them.

## Network, Analytics, Ads, and Tracking

This release does not declare the Android `INTERNET` permission. WA Schedule does not include analytics SDKs, advertising SDKs, crash-reporting SDKs, billing SDKs, or developer-operated telemetry.

## Notifications

WA Schedule may show local Android notifications about scheduled automation status, permission status, or execution results.

## Data Deletion

You can delete stored data by deleting rules/history where available, clearing app storage in Android settings, or uninstalling the app.

## Contact

alexapp.support@gmail.com
