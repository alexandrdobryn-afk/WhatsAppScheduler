# Data Safety Draft

Use this as a Play Console draft. Final answers must match the published Privacy Policy and the exact release binary.

## Collection

Does the app collect user data?

Current recommendation: Developer-side collection: NO.

Reason:

- The app does not declare `INTERNET`.
- Source/dependency scans did not find analytics, ads, crash reporting, billing, Firebase, Sentry, Retrofit, OkHttp networking, AppsFlyer, or Adjust SDKs.
- Rules, messages, schedules, preferences, and execution history stay on device.
- Diagnostic reports are generated locally and are not sent to the developer unless the user manually shares them outside the app.

## Sharing

Does the app share user data?

Current recommendation: Sharing: NO.

Explanation:

WA Schedule does not share user data with the developer. User-authored messages are transferred to WhatsApp as a user-directed core action, covered by the user-created rule and prominent disclosure/consent. For Test Send and scheduled send, WA Schedule places the saved message into the official WhatsApp app so WhatsApp can send it to the user-selected chat or group. This is not developer-side collection and is not a transfer to WA Schedule servers.

## Data Types Stored Locally

- User-generated content: message text entered by the user.
- App activity: local execution history, timestamps, statuses, and structured error codes.
- App info and performance: local diagnostic report metadata when the user generates a report.
- Other user-provided identifiers: chat/group names typed by the user. The app does not request Android Contacts permission.

## Security Practices

- Data encrypted in transit: Not applicable for developer transmission because the app has no network permission.
- Data deletion request mechanism: No account system. Users can delete local data by deleting rules/history, clearing app data, or uninstalling.
- Independent security review: Not performed.

Final Play Console wording note:

User-authored scheduled content is transferred to WhatsApp only as the explicit, user-directed core action of a saved rule, Dry Run/Test Send path, or scheduled send. It is not sent to the developer, not sent to WA Schedule servers, and should not be represented as developer-side collection or sharing with the developer.

Data Safety status: READY as a draft; final Play Console selections still need manual review against the exact current Console wording.
