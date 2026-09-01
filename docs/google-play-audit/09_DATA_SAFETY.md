# Data Safety Draft

Use this as a Play Console draft. Final answers must match the published Privacy Policy and the exact release binary.

## Collection

Does the app collect user data?

Recommended answer for current release: No developer-side collection.

Reason:

- The app does not declare `INTERNET`.
- Source/dependency scans did not find analytics, ads, crash reporting, billing, Firebase, Sentry, Retrofit, OkHttp networking, AppsFlyer, or Adjust SDKs.
- Rules, messages, schedules, preferences, and execution history stay on device.
- Diagnostic reports are generated locally and are not sent to the developer unless the user manually shares them outside the app.

## Sharing

Does the app share user data?

Recommended explanation:

WA Schedule does not share user data with the developer. For Test Send and scheduled send, the app transfers the user-authored message into the official WhatsApp app so WhatsApp can send it to the user-selected chat or group. This is a user-directed transfer that is the app's core feature, not developer-side collection.

Final Play Console answer: NEEDS REVIEW against the exact current Console wording.

## Data Types Stored Locally

- User-generated content: message text entered by the user.
- App activity: local execution history, timestamps, statuses, and structured error codes.
- App info and performance: local diagnostic report metadata when the user generates a report.
- Other user-provided identifiers: chat/group names typed by the user. The app does not request Android Contacts permission.

## Security Practices

- Data encrypted in transit: Not applicable for developer transmission because the app has no network permission.
- Data deletion request mechanism: No account system. Users can delete local data by deleting rules/history, clearing app data, or uninstalling.
- Independent security review: Not performed.

Data Safety status: READY as a draft; NEEDS REVIEW in Play Console.
