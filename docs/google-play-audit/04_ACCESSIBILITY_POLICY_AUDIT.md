# Accessibility Policy Audit

Service:

- `WhatsAppAccessibilityService`
- Manifest permission: `android.permission.BIND_ACCESSIBILITY_SERVICE`
- Config: `app/src/main/res/xml/accessibility_service_config.xml`
- Scoped package after remediation: `com.whatsapp`

AccessibilityService configuration after remediation:

- `canRetrieveWindowContent = true`
- `accessibilityEventTypes = typeWindowStateChanged | typeWindowContentChanged | typeViewClicked | typeViewFocused`
- `accessibilityFeedbackType = feedbackGeneric`
- `accessibilityFlags = flagReportViewIds`
- `notificationTimeout = 100`
- `packageNames = com.whatsapp`

Removed:

- `com.whatsapp.w4b`
- `flagRetrieveInteractiveWindows`

Policy classification:

- The app is not an accessibility tool for users with disabilities.
- It uses AccessibilityService for deterministic, user-directed automation.
- The user defines target chat/group, message, schedule, enabled state, and test/send actions.
- No autonomous decision-making was found in source review.

Source review confirmations:

- No coordinate automation found.
- No `dispatchGesture` usage found.
- No clipboard automation found.
- No scraping of unrelated applications found.
- No `INTERNET` permission found.
- Accessibility events are guarded to supported WhatsApp package names.
- Automation reads `rootInActiveWindow`, verifies target chat title, finds semantic nodes, sets user-authored text, clicks send, and returns Home.
- Full message text is not logged by the reviewed code path.

Remaining Play review needs:

1. Complete Accessibility API declaration in Play Console.
2. Provide the review video.
3. Keep the disclosure and declaration aligned with the current consumer-WhatsApp-only scope.
4. Do not claim WhatsApp Business support unless it is re-added and runtime-tested.

Accessibility declaration readiness: READY for submission drafting, subject to Google review.

Runtime status: NOT TESTED.
