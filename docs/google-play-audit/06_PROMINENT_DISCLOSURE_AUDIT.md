# Prominent Disclosure Audit

Current implementation:

- Accessibility disclosure strings exist in English, Russian, and Ukrainian resources.
- Disclosure is shown from onboarding before opening Android Accessibility settings.
- Disclosure is shown from settings before opening Android Accessibility settings.
- Dialog includes a cancel/decline path.
- Dialog includes an explicit continue/consent action.

Remediation result:

- Disclosure must now refer only to official consumer WhatsApp support.
- It must not mention WhatsApp Business unless support is re-added and tested.
- It must not imply that data is sent to the developer.

Recommended final disclosure:

WA Schedule uses Android Accessibility permission to operate the official WhatsApp app only for rules you create. When a rule runs, it may inspect the visible WhatsApp screen to verify the selected chat or group, find the message box and send button, enter your saved message, tap Send, and return to Home. WA Schedule does not read your conversation history for developer use, does not collect or sell this data, and does not send it to our servers. Rules, preferences, and history stay on this device unless you manually share a diagnostic report.

Status:

- Placement: PASS
- Explicit consent: PASS
- Decline path: PASS
- Final wording in app resources: NEEDS REVIEW if the exact recommended wording is required before Play upload.
