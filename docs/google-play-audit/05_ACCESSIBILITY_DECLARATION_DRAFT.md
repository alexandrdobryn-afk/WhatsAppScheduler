# Accessibility Declaration Draft

Use this draft in Play Console only after confirming it still matches the exact release binary.

App name: WA Schedule

AccessibilityService name: WA Schedule Accessibility Service

Is the app an accessibility tool?

No. WA Schedule is a productivity automation app. It is not primarily designed as assistive technology for users with disabilities.

Why does the app use AccessibilityService?

WA Schedule uses AccessibilityService to complete only WhatsApp send flows that the user explicitly configured. The service opens the official consumer WhatsApp app, verifies the selected chat or group, locates the message field and send button, enters the user-authored message, taps Send, and returns to Home.

Supported package scope:

- `com.whatsapp`

Unsupported/not declared:

- WhatsApp Business (`com.whatsapp.w4b`) is not in scope for this release.
- The app does not inspect unrelated apps.
- The app does not use coordinate automation.
- The app does not use gesture abuse.
- The app does not use clipboard automation.
- The app does not autonomously choose recipients or message content.

What user data is accessed through AccessibilityService?

The service may transiently inspect the currently visible WhatsApp screen to verify the user-entered target chat/group and locate the message input and send button. It does not intentionally read or store WhatsApp conversation history, incoming messages, or Accessibility node trees.

What data is stored?

The app stores rules, target names, user-authored message text, schedule details, retry settings, app preferences, and local execution history on the device. Diagnostic export may include app/device state, permission state, Accessibility connection state, rule count, recent execution status, structured error codes, and limited rule metadata.

What data is transmitted to the developer?

None. This release does not declare `INTERNET` and does not include analytics, ads, crash-reporting, billing, or telemetry SDKs.

What data is transferred to WhatsApp?

When the user runs Test Send or a scheduled send, WA Schedule places the user-authored message into WhatsApp for the selected chat or group. This is the core user-directed transfer requested by the user-created rule.

Can users opt out?

Yes. Users can decline the in-app disclosure, avoid enabling AccessibilityService, disable the service in Android settings, delete rules, clear app data, or uninstall the app.
