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
8. Open Settings -> About -> Privacy Policy and show that the public policy opens.
9. Open Settings -> About -> Support and show the support email action.
10. Create a new rule with a normal WhatsApp group target.
11. Enter a non-sensitive user-authored message.
12. Save the rule.
13. Dry Run verifies target and does not send.
14. Test Send or near-future scheduled send sends only to the configured target.
15. WhatsApp returns to Home/background after success.
16. WA Schedule history/home shows execution status.
17. Disable AccessibilityService in Android settings.

Reviewer message:

WA Schedule uses AccessibilityService only for deterministic, user-configured automation in the official consumer WhatsApp app. It verifies the selected target, enters the user-authored message, taps Send, and returns Home. It does not collect or transmit Accessibility-derived data to the developer.
