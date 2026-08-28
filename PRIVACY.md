# WA Schedule Privacy Policy

Last updated: 2026-08-28

WA Schedule is a local Android app for scheduling deterministic WhatsApp actions
configured by the phone owner.

## Data Stored On The Device

WA Schedule stores the following data locally on the Android device:

- schedule names;
- target chat or group names typed by the user;
- message text typed by the user;
- selected schedule type, dates, weekdays, times, timezone, and retry settings;
- execution history, including timestamp, target name, status, attempt number,
  and structured error code.

This data is stored in the app's private local storage using Room and DataStore.

## Data Not Collected

WA Schedule does not:

- operate a backend server;
- send schedule data to the developer or any third-party server;
- sell data;
- use analytics or advertising SDKs;
- request the Android `INTERNET` permission;
- read or store WhatsApp conversation history;
- store messages received from other people;
- upload Accessibility UI trees or screen contents.

## Accessibility Service

WA Schedule uses Android Accessibility Service only to perform actions explicitly
configured by the user:

- open the official WhatsApp app;
- find the chat or group name entered by the user;
- enter the prepared message text entered by the user;
- press Send at the scheduled time.

The Accessibility service is restricted to supported WhatsApp packages and is not
used to inspect unrelated apps.

## Notifications

WA Schedule may show local Android notifications for send results and errors.
Notification settings are stored locally.

## Data Deletion

Deleting a schedule removes its future schedule data from the app database.
Uninstalling WA Schedule removes the app's private local data from the device.

## Contact

For support or privacy questions, use the project's GitHub issue tracker.
