# App Access

Login required: No

Account required: No

Subscription required: No

Special access required for review:

- Android Accessibility permission must be enabled for send automation.
- Exact alarm permission may need to be granted depending on Android version and app state.
- Notification permission may be requested on Android 13+.
- WhatsApp must be installed and available.

Reviewer instructions draft:

1. Install WA Schedule.
2. Install WhatsApp and sign in with a test WhatsApp account.
3. Create or open a normal WhatsApp group, not a Community.
4. Launch WA Schedule and complete onboarding.
5. Accept the prominent Accessibility disclosure.
6. Enable WA Schedule in Android Accessibility settings.
7. Open Settings -> About -> Privacy Policy and verify it opens the public policy URL in a browser.
8. Open Settings -> About -> Support and verify the mail client opens `alexapp.support@gmail.com`.
9. Create a rule targeting the test WhatsApp group.
10. Use Dry Run to verify the target without sending.
11. Use Test Send or a near-future schedule to verify a real send.
12. Confirm WA Schedule returns to Home after the send.

Credentials to provide in Play Console:

- None for WA Schedule.
- WhatsApp account credentials cannot safely be provided by the app developer unless a dedicated review device/account is prepared.

Risk:

Review may be blocked if the reviewer cannot set up WhatsApp or cannot enable Accessibility. Provide a clear review video and written instructions.
