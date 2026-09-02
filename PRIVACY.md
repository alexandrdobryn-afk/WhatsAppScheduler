# WA Schedule Privacy Policy

Last updated: 2026-09-02

Public policy URL: https://alexandrdobryn-afk.github.io/WhatsAppScheduler/privacy/

Support contact: alexapp.support@gmail.com

WA Schedule is a local Android application for scheduling deterministic WhatsApp
actions configured by the phone owner. WA Schedule is independent and is not
affiliated with, endorsed by, or sponsored by WhatsApp LLC or Meta Platforms,
Inc.

## 1. Overview

WA Schedule lets a user create local schedule rules that open the official
consumer WhatsApp Android app, verify a configured chat or group, enter a
message written by the user, and send it at the selected time. The app is
designed for local, user-directed scheduling. It does not operate a developer
backend and does not request the Android `INTERNET` permission.

## 2. Information Stored On The Device

WA Schedule stores the following data locally on the Android device:

- schedule names;
- target chat or group names typed by the user;
- message text typed by the user;
- selected schedule type, dates, weekdays, times, timezone, and retry settings;
- execution history, including timestamp, target name, status, attempt number,
  and structured error code;
- app preferences such as onboarding state, language, and settings.

Rule, message, schedule, and execution-history data is stored in the app's
private local Room database. App preferences are stored locally with DataStore.

## 3. Scheduled Messages And Chat Names

Message text and chat or group names are entered by the user and stored only so
the app can execute the user's saved schedule or explicit test-send action. WA
Schedule does not request Android Contacts permission and does not upload chat
names or message text to the developer.

## 4. Accessibility Service

WA Schedule uses Android Accessibility Service only to perform WhatsApp actions
explicitly configured by the user. The Accessibility service is restricted to
the official consumer WhatsApp Android package (`com.whatsapp`) and is not used
to inspect unrelated apps.

## 5. How Accessibility Is Used

When Dry Run, Test Send, or a scheduled send runs, WA Schedule may transiently
inspect the currently visible WhatsApp screen to:

- verify the target chat or group selected by the user;
- locate the message input field;
- enter the prepared message text entered by the user;
- locate the send button;
- press Send for Test Send and scheduled send;
- return the device to Home after the action.

WA Schedule does not store WhatsApp conversation history, does not store
Accessibility node trees, and does not upload Accessibility-derived screen
contents to the developer.

## 6. Data Collection

WA Schedule does not collect schedule, message, diagnostic, or
Accessibility-derived data for the developer. The current release has no
developer-operated backend, no analytics SDK, no advertising SDK, no
crash-reporting SDK, no billing SDK, and no Android `INTERNET` permission.

## 7. Data Sharing

WA Schedule does not share user data with the developer. For Test Send and
scheduled send, the app transfers the user-authored message into the official
WhatsApp app so WhatsApp can send it to the user-selected chat or group. This is
a user-directed transfer initiated by the user's saved rule or explicit action,
not developer-side collection.

## 8. Network And Backend

WA Schedule does not operate a backend server and does not declare the Android
`INTERNET` permission. The app's scheduling rules, preferences, diagnostics, and
execution history remain local to the device unless the user manually shares
diagnostic files outside the app.

## 9. Analytics And Advertising

WA Schedule does not include analytics, advertising, tracking, attribution,
marketing, or behavioral profiling SDKs.

## 10. Local Storage And Retention

Local rules, message text, chat or group names, preferences, and execution
history remain on the device while the app is installed, unless the user deletes
them or clears app storage. Diagnostic reports, when created by the user, are
written to the app-specific external files area on the same device.

## 11. Data Deletion

Deleting a schedule removes its future schedule data from the app database.
Users can remove local app data by deleting rules and history where available,
clearing WA Schedule storage in Android settings, or uninstalling WA Schedule.
Uninstalling the app removes its private local data from the device.

## 12. Security

WA Schedule stores app data in Android app-private storage. The app does not
publish a cloud account system and does not transmit app data to a developer
server. Device-level protections, backups, and local storage behavior are
controlled by Android and the user's device settings.

## 13. Children's Privacy

WA Schedule is not designed for children and does not knowingly collect personal
data from children. The app has no account system, backend, analytics, ads, or
developer-side data collection in the current release.

## 14. Changes To This Privacy Policy

This Privacy Policy may be updated when WA Schedule changes its data handling,
permissions, supported integrations, or release status. The "Last updated" date
will be changed when the policy is updated.

## 15. Contact

alexapp.support@gmail.com
