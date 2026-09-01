# Review Video Checklist

Purpose:

Prepare the video required for Accessibility API review.

Video must match the remediated app scope:

- Official consumer WhatsApp only: `com.whatsapp`.
- Do not show or claim WhatsApp Business support.
- Do not show unrelated apps being inspected.
- Do not show private personal chats, phone numbers, or sensitive message text.

Video should show:

1. Install/open the release build.
2. First-launch onboarding.
3. Prominent Accessibility disclosure before Android settings.
4. User taps affirmative consent.
5. Android Accessibility settings screen.
6. WA Schedule AccessibilityService enabled.
7. Return to WA Schedule.
8. Create a new rule with a normal WhatsApp group target.
9. Enter a non-sensitive user-authored message.
10. Save the rule.
11. Dry Run verifies target and does not send.
12. Test Send or near-future scheduled send sends only to the configured target.
13. WhatsApp returns to Home/background after success.
14. WA Schedule history/home shows execution status.
15. Disable AccessibilityService in Android settings.

Reviewer message:

WA Schedule uses AccessibilityService only for deterministic, user-configured automation in the official consumer WhatsApp app. It verifies the selected target, enters the user-authored message, taps Send, and returns Home. It does not collect or transmit Accessibility-derived data to the developer.
