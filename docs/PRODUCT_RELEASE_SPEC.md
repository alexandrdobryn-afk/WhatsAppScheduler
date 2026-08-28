# WA Schedule - Product Release / Google Play Ready Specification

This document is the product-release specification for WA Schedule. It should be
treated as the authoritative requirements checklist for release-readiness work.

## 1. Goal

Improve the existing WA Schedule app without rewriting the project from
scratch.

The app must become a full local Android WhatsApp message scheduler for a
personal WhatsApp account:

- users create schedules;
- users choose a specific chat or group;
- users define the message;
- users define specific dates and/or weekdays;
- users define one or more times;
- the app executes the predefined action independently;
- execution works when the app UI is closed or backgrounded;
- execution supports screen-off scenarios without secure lock;
- the app records history;
- the app diagnoses problems;
- Accessibility behavior is safe and understandable;
- the architecture and legal disclosures are prepared for an attempted Google
  Play publication.

## 2. Schedule Model

The current weekday-centric schedule model is insufficient. Three schedule types
must be supported.

### 2.1 Weekly

Example:

```text
Every Monday, Wednesday, and Friday
08:00
12:00
18:00
```

The user can choose any combination of:

```text
Mon Tue Wed Thu Fri Sat Sun
```

Quick presets:

- Every day;
- Mon-Fri;
- Weekends.

### 2.2 Specific Date

Example:

```text
25.08.2026
14:30
```

This is a one-time message. After successful execution:

```text
status = COMPLETED
```

The rule must not be scheduled again.

### 2.3 Multiple Dates

Example:

```text
25.08.2026
28.08.2026
01.09.2026
```

The same message can be used with common times or date-specific times, depending
on the implementation chosen for this release.

## 3. Schedule Picker UI

In the rule editor, add:

```text
Schedule type

Weekly
Specific date
Multiple dates
```

For Weekly, show weekdays.

For Specific date:

```text
Date
25 Aug 2026
```

For Multiple dates:

```text
Dates
25 Aug 2026
28 Aug 2026
01 Sep 2026

+ Add date
```

## 4. Multiple Times

For any schedule type, the user can add multiple times:

```text
08:00
09:00
10:00
```

UI:

```text
Times

08:00 x
09:00 x
10:00 x

+ Add time
```

Requirements:

- at least one time;
- duplicates are forbidden;
- times are sorted;
- store as `LocalTime`.

## 5. WhatsApp Target

Do not limit terminology to "Group".

Use:

```text
WhatsApp target
```

or:

```text
Chat / group
```

Support:

- direct chats;
- regular WhatsApp groups.

Community nested groups can remain unsupported unless reliable two-level
navigation is implemented.

For the current release, state explicitly:

```text
Supported:
- Direct chats
- Regular WhatsApp groups

Experimental / unsupported:
- Community nested groups
```

Do not claim a function is supported if it was not verified.

## 6. Target Selection

The first release can keep manual target-name entry:

```text
Chat / group name
```

Add:

```text
Test target
```

The button must:

```text
open WhatsApp
-> find target
-> open target
-> verify title
-> not send
```

Result:

```text
Target found
Chat verified
Input available
Ready
```

## 7. Target Validation

Before sending, always require:

```text
actual chat title == expected title
```

If several matches exist:

```text
AMBIGUOUS_CHAT
```

If none exist:

```text
CHAT_NOT_FOUND
```

Never send to a similar chat.

## 8. Rule Editor

The editor must be properly scrollable.

Structure:

```text
New schedule / Edit schedule

Name
Chat / group
Message
Schedule type
Dates / weekdays
Times
Allowed delay
Active

[Test without sending]
[Test send]

[Save]
```

For edit:

```text
Menu
Edit
Delete
```

## 9. Save

The Save button must always be reachable.

On Save:

```text
validate
-> Room transaction
-> save/update Rule
-> save times/dates
-> scheduler.rescheduleNext()
-> Home
```

## 10. Unsaved Changes

If the user changes the form and leaves:

```text
Unsaved changes

Discard
Cancel
Save
```

No silent data loss.

## 11. Home

The Home screen should be compact.

Show:

```text
WA Schedule

Automation
ON

Next message
Today 21:00
Work Group
"+"

Schedules
```

Rule card:

```text
Hourly report

Work Group
"+"

Mon-Fri
08:00 09:00 10:00

ON                         Menu
```

## 12. Rule Menu

Each rule has a menu:

```text
Edit
Enable / Disable
Duplicate
Delete
```

Duplicate lets the user quickly create a similar schedule for another target.

## 13. Delete

Deletion requires confirmation:

```text
Delete schedule?

Future sends for this schedule will be cancelled.

Cancel
Delete
```

After deletion:

```text
Room delete
-> rescheduleNext()
```

## 14. Duplicate

On duplicate:

- create a new Rule ID;
- copy message;
- copy times;
- copy days/dates;
- copy allowed delay;
- copy name as `Original name (copy)`;
- open the editor for the copy.

## 15. Global Automation

Home must expose:

```text
Automatic sending
ON / OFF
```

This is a global master switch.

If OFF:

- rules remain saved;
- scheduled execution does not send;
- UI clearly shows `Automation paused`.

## 16. Pause Individual Rule

Each rule has:

```text
Active ON/OFF
```

Disabling:

- does not delete the rule;
- excludes it from scheduler calculations.

Re-enabling recalculates the next occurrence.

## 17. Next Execution

For each active rule, calculate:

```text
Next:
Today 21:00
```

or:

```text
Tomorrow 08:00
```

Home must show the nearest global event among all rules.

## 18. History

History must be useful, not just a raw list.

For each execution:

```text
20:00
Work Group
"+"

SENT
```

or:

```text
20:00
Work Group

FAILED
ACCESSIBILITY_DISABLED
```

## 19. History Filters

Add:

```text
All
Sent
Failed
Skipped
```

## 20. Execution Details

On click:

```text
Scheduled
20:00:00

Started
20:00:03

Finished
20:00:06

Target
Work Group

Status
SENT

Attempt
1
```

On error:

```text
Error
DEVICE_SECURE_LOCKED
```

## 21. Retry

Retry only recoverable errors, for example:

- `DEVICE_LOCKED`;
- `NO_NETWORK`;
- `ACCESSIBILITY_NOT_CONNECTED`.

Do not retry:

- `CHAT_NOT_FOUND`;
- `AMBIGUOUS_CHAT`;
- `WRONG_CHAT`;

unless evidence shows the condition is temporary.

## 22. Allowed Delay

If scheduled time is:

```text
20:00
```

and allowed delay is:

```text
10 minutes
```

the app may retry until:

```text
20:10
```

After that:

```text
SKIPPED / MISSED_WINDOW
```

Do not send the message an hour late.

## 23. Background Execution

The Activity must not be part of the execution pipeline.

Execution must work when:

- app is open;
- app is backgrounded;
- app is removed from Recents;
- Activity is destroyed.

## 24. Screen Off

Support:

- screen off;
- non-secure keyguard.

Pipeline:

```text
Alarm
-> wake
-> non-secure lock dismiss
-> WhatsApp
-> send
-> HOME
```

After execution, do not keep the display on. The screen should turn off again by
normal system timeout.

## 25. Secure Lock

If PIN, password, or pattern is active:

```text
DEVICE_SECURE_LOCKED
```

Do not bypass it. Retry within allowed delay.

## 26. Accessibility Lifecycle

Do not use:

```kotlin
serviceInstance == null
```

as the only OFF signal.

Distinguish:

- permission off;
- permission on, disconnected;
- connected;
- interrupted;
- destroyed.

## 27. Accessibility Health

Diagnostics:

```text
Accessibility permission: ON
Accessibility connection: CONNECTED
Last connected: 20:31
```

## 28. WhatsApp Cleanup

After Send:

```text
verify
-> SUCCESS
-> GLOBAL_ACTION_HOME
```

WhatsApp is minimized. Do not force-stop it.

## 29. Permissions UI

Rework onboarding.

Each permission should be a separate card:

```text
Accessibility
Required for WhatsApp automation

Not enabled
[Open settings]
```

```text
Notifications
Optional

Enabled
OK
```

## 30. Permission Status

Use:

- green for OK;
- yellow for recommended;
- red for blocking.

Battery optimization is recommended, not a red blocker.

## 31. Language

Fix the crash.

Support:

- System;
- Russian;
- Ukrainian;
- English.

Use the standard Android per-app locale mechanism.

## 32. Theme

Support:

- System;
- Light;
- Dark.

## 33. Time Zone

Support:

- Device timezone;
- Custom timezone.

The scheduler must actually use the selected timezone.

## 34. Settings

Structure:

```text
General
Automation
Schedule
Permissions
Security
Diagnostics
About
```

## 35. Security

Add optional App Lock:

```text
App lock
OFF / ON
```

When enabled, use biometric and/or device credential.

Protect:

- rule changes;
- Delete;
- Test Send;
- global automation toggle;
- Settings.

Scheduled execution still works without opening the UI.

## 36. Privacy

The app must not:

- read WhatsApp history;
- store other people's messages;
- send UI content to a server;
- have its own backend.

## 37. INTERNET Permission

If the app does not need direct internet access, do not add:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

WhatsApp uses the network itself. `ACCESS_NETWORK_STATE` can remain for
prechecks.

## 38. Accessibility Scope

The AccessibilityService must be limited to:

```text
com.whatsapp
```

and only if Business is actually supported:

```text
com.whatsapp.w4b
```

If Business is not tested, do not claim support.

## 39. Google Play Disclosure

Before opening Android Accessibility Settings, show a separate screen:

```text
WA Schedule uses Android Accessibility Service to perform only the actions you configure:

- open WhatsApp;
- find the chat/group you specified;
- enter your prepared message;
- press Send at the scheduled time.

WA Schedule does not read or store your conversations.

[Cancel]
[I understand and continue]
```

## 40. Accessibility Declaration

Prepare Play Console text explaining:

- the app is not intended primarily for people with disabilities;
- Accessibility is used for automation;
- automation is deterministic;
- the user defines target, text, date, and time;
- the app does not make autonomous sending decisions.

## 41. Privacy Policy

Create:

```text
PRIVACY.md
```

and a public web version.

State:

- what data is stored;
- where it is stored;
- what is not sent;
- Accessibility is used locally;
- no sale of data;
- no backend.

## 42. Google Play Data Safety

Prepare Data Safety answers based on actual behavior.

Do not claim that no data is collected if analytics or crash reporting are
added later.

If analytics is not needed, do not add it for the first release.

## 43. Release Build

Create a release build with:

```kotlin
minifyEnabled = true
shrinkResources = true
```

## 44. Signing

Use a separate release keystore.

Never commit:

- `*.jks`;
- `keystore.properties`;
- passwords.

## 45. Package Name

Before public Play release, replace:

```text
com.example.wascheduler
```

with a permanent `applicationId`, for example:

```text
io.github.alexandrdobryn.waschedule
```

After the first production release, it must not be changed.

## 46. Versioning

Use:

```text
versionCode
versionName
```

Example:

```text
versionCode = 1
versionName = "1.0.0"
```

## 47. About

Add:

```text
WA Schedule
Version 1.0.0

GitHub
Privacy Policy
Open-source licenses
Report issue
```

## 48. Error UX

Do not show technical-only errors like:

```text
ACCESSIBILITY_DISABLED
```

as the only user message.

UI:

```text
Accessibility service is disabled.
Open Android settings and enable WA Schedule.

[Open settings]
```

Technical codes can remain in Details.

## 49. Diagnostics

Diagnostics must show:

- app version;
- Android version;
- device manufacturer;
- WhatsApp version;
- Accessibility permission;
- Accessibility connection;
- exact alarms;
- notifications;
- battery optimization;
- screen state;
- keyguard state;
- secure lock;
- timezone;
- enabled schedules;
- next execution;
- last execution;
- last error.

## 50. Export Diagnostic Report

Add:

```text
Export diagnostic report
```

It creates text/JSON without personal WhatsApp chat contents.

## 51. WhatsApp Compatibility Test

Add:

```text
Check WhatsApp compatibility
```

Without sending, verify:

- WhatsApp launches;
- search is available;
- chat list is recognizable.

## 52. Scheduled Automation Test

Create an internal/manual acceptance scenario:

```text
Rule:
Test

Target:
Test group

Message:
WA_TEST

Time:
+2 minutes
```

Verify the full pipeline.

## 53. Reliability Test

Do not consider the app ready after one message.

Run at least 30 scheduled sends in different states:

- foreground;
- background;
- screen off;
- WhatsApp already open;
- another chat open.

## 54. Regression Tests

Required unit tests:

- next weekly occurrence;
- specific date;
- multiple dates;
- timezone;
- Sunday;
- DST;
- allowed delay;
- retries;
- duplicate occurrence;
- disabled rule.

## 55. Room Tests

Verify:

- create;
- update;
- delete;
- duplicate;
- rule plus times transaction;
- rule plus dates transaction.

## 56. UI Tests

Verify:

- all seven weekdays visible;
- Sunday selectable;
- Save reachable;
- delete works;
- duplicate works;
- dirty form dialog;
- language switching;
- permissions screen;
- small-screen layout.

## 57. Instrumentation

Minimum:

```text
Pixel 8
Android 15
API 35
```

## 58. Real Device

Verify on at least one physical Android device.

Especially:

- Xiaomi/HyperOS;
- Accessibility;
- background restrictions;
- screen off;
- sideload restrictions.

## 59. Google Play Preparation

Build:

```text
AAB
```

not only APK.

Keep APK for GitHub distribution.

## 60. Play Listing

Prepare:

- app name;
- short description;
- full description;
- screenshots;
- icon;
- feature graphic;
- Privacy Policy URL;
- Accessibility disclosure;
- Data Safety;
- support email.

## 61. Main Project Rule

Do not add features only for quantity.

WA Schedule must stay:

- simple;
- predictable;
- local;
- understandable.

Core value:

```text
I define once who, what, and when to send - then the app performs it itself.
```

## 62. Definition of Done

Do not consider the release task complete until this actually passes:

```text
Install
-> Onboarding
-> Permissions
-> Create weekly rule
-> Save
-> Edit
-> Delete
-> Create one-time rule
-> Background
-> Screen off
-> Wake
-> Send
-> WhatsApp minimized
-> History
-> Restart
-> Rule still exists
```
