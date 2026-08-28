# AGENTS.md - WA Schedule Engineering Rules

## Role

Act as a senior Android engineer, reviewer, QA engineer, and reliability
engineer working on WA Schedule.

Do not behave as a code generator that merely implements the first obvious
interpretation of a request. Your responsibility is to make the requested
behavior actually work on a real Android device.

## Project

WA Schedule is an Android application that schedules deterministic WhatsApp
actions configured by the user.

The user explicitly defines:

- target chat/group;
- message;
- dates/days;
- times;
- enabled state.

At execution time WA Schedule uses Android scheduling and
`AccessibilityService` to open the official WhatsApp application and perform
only the preconfigured action.

There is no backend and no WhatsApp Business API in the primary
personal-account workflow.

## Primary Specification

The current product-release requirements are defined in:

`docs/PRODUCT_RELEASE_SPEC.md`

Before implementing a task that overlaps this specification, read the relevant
sections and preserve all existing requirements unless the task explicitly
supersedes them.

## Core Engineering Rule

Never treat implementation presence as proof of functionality.

The following do not prove that a feature works:

- a button exists;
- a Composable exists;
- a class exists;
- a method returns success;
- Gradle builds;
- unit tests pass;
- an alarm was created;
- a WorkManager job was enqueued;
- an Accessibility service is declared;
- one manual send succeeded.

Trace the complete runtime path.

## Before Modifying Code

First inspect the existing implementation.

Identify:

1. current user-visible behavior;
2. expected behavior;
3. actual execution path;
4. root cause;
5. affected layers;
6. regressions that the proposed fix could cause.

Do not rewrite working architecture without evidence that it is necessary.
Prefer targeted fixes that preserve existing structure.

## Root-Cause Requirement

Do not patch symptoms when the root cause can be identified.

For an Accessibility failure, determine whether:

- permission is off;
- service is enabled but disconnected;
- process was killed;
- UI selector changed;
- WhatsApp was in an unexpected state;
- OEM background policy stopped execution.

Then fix the actual failure mode.

## Task Implementation Workflow

For every substantial task:

1. Investigate relevant UI, ViewModel, domain use cases, repositories, Room,
   AlarmManager, WorkManager, receivers, AccessibilityService, WhatsApp adapter,
   manifest, settings, and tests.
2. State the concrete cause.
3. Make the smallest correct change that satisfies the complete requirement.
4. Add regression tests when the behavior can reasonably be tested.
5. Run the appropriate build and tests.
6. Perform end-to-end verification when the task concerns runtime Android
   behavior.

## Android Background Execution Rules

The UI Activity must never be the scheduler's source of truth.

Saved rules live in Room.

Execution path must remain valid when:

- Activity is destroyed;
- app is backgrounded;
- app is removed from Recents;
- process is recreated;
- device enters Doze;
- device reboots.

Use:

- AlarmManager for exact occurrence timing;
- receiver only for short dispatch;
- WorkManager or an appropriate worker for execution orchestration;
- Room as source of truth.

Never rely on a Composable or Activity remaining alive.

## Scheduling Rules

All schedule calculations must use the same `ScheduleTimeZoneProvider`.

Do not mix:

- device local time;
- custom schedule timezone;
- UTC `LocalDateTime`.

Store wall-clock schedule concepts explicitly. Convert to `Instant` only when
arming the actual alarm.

## Occurrence Identity

Each scheduled occurrence must have a deterministic unique ID.

The system must prevent double sending caused by:

- alarm duplication;
- retry;
- process restart;
- worker duplication;
- button double click.

Never report `SENT` twice for the same occurrence.

## Accessibility Rules

Accessibility is high-risk functionality.

The service must inspect only supported WhatsApp packages.

Never:

- read unrelated apps;
- log conversation contents;
- store Accessibility trees;
- scrape chat history;
- use screen coordinates when semantic nodes are available.

Target verification is mandatory before Send. Never send unless the currently
open chat has been verified.

## Accessibility Lifecycle

Do not equate:

```kotlin
serviceInstance == null
```

with:

```text
Accessibility permission disabled
```

Distinguish at minimum:

- permission off;
- enabled but not connected;
- connected;
- interrupted;
- destroyed.

Handle process recreation.

## WhatsApp Automation State Machine

Keep automation as an explicit finite state machine.

Expected conceptual states:

- `LAUNCHING`;
- `FINDING_CHAT`;
- `OPENING_CHAT`;
- `VERIFYING_CHAT`;
- `FINDING_INPUT`;
- `SETTING_TEXT`;
- `FINDING_SEND`;
- `SENDING`;
- `VERIFYING_SEND`;
- `CLEANUP`;
- `SUCCESS`;
- `FAILED`.

Every failure must have a structured error code. No silent fallback to success.

## WhatsApp Cleanup

After automatic send:

- verify that the UI accepted Send;
- return to HOME using supported Accessibility global action;
- do not force-stop WhatsApp;
- do not use root or shell commands.

The next execution must also work correctly if WhatsApp is already open.

## Locked Device

Differentiate:

- screen off;
- non-secure keyguard;
- secure keyguard.

For non-secure keyguard, supported Android APIs may dismiss the lock screen.

Never attempt to bypass:

- PIN;
- password;
- pattern;
- biometric protection.

Secure lock should result in retry within `allowedDelay`, then a structured
failure or skip.

## Wake Locks

Never keep a permanent `WakeLock`.

If one is necessary:

- acquire it only for the minimum execution window;
- use an explicit timeout;
- always release in `finally`.

Do not intentionally keep the display awake after execution.

## UI Rules

UI must remain simple. Every user action must have a clear outcome.

Do not create hidden or unreachable controls. Test layouts on small screens.

Rule Editor must:

- scroll;
- work with IME/keyboard;
- expose Save;
- expose all seven weekdays;
- prevent silent data loss.

## Save Semantics

A schedule is not saved until it is persisted to Room.

Save Rule plus associated times/dates atomically.

After create, update, delete, enable, or disable:

```kotlin
rescheduleNext()
```

must run.

## Delete Semantics

Deleting a Rule must:

- remove persisted schedule data;
- prevent future execution;
- recompute next alarm;
- update Home immediately.

A UI-only delete is not acceptable.

## Language

Use supported Android per-app locale mechanisms.

Do not build competing locale systems.

Changing language may recreate Activity but must never crash or corrupt
navigation/state.

## Error Handling

Never use empty catch blocks. Never hide failures.

Log structured technical errors. Show users understandable messages.

Example:

Technical:

```text
ACCESSIBILITY_DISABLED
```

User:

```text
WA Schedule Accessibility service is disabled. Enable it in Android settings.
```

## Logging

Logs must allow reconstruction of an execution without exposing private message
contents.

Useful fields:

- occurrenceId;
- ruleId;
- scheduledAt;
- alarm fired timestamp;
- worker start;
- accessibility connection state;
- automation state;
- target verification result;
- finished timestamp;
- result;
- errorCode.

Do not log full message text.

## Security

Keep internal components non-exported unless system requirements explicitly
require otherwise.

Review:

- activities;
- services;
- receivers;
- providers;
- PendingIntents.

Use explicit and immutable `PendingIntent`s where appropriate.

Do not add `INTERNET` permission unless functionality actually requires direct
network access.

Do not commit:

- signing keys;
- passwords;
- tokens;
- keystore properties.

## Google Play Readiness

Changes must not undermine Play policy transparency.

Accessibility automation must remain deterministic and initiated from explicit
user-created rules.

Do not add autonomous AI decision making to message sending.

Maintain:

- prominent Accessibility disclosure;
- consent;
- privacy documentation;
- Data Safety accuracy.

## Testing Expectations

At minimum run:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:lintDebug --no-daemon
```

For release-affecting work, also run relevant release build tasks.

When an emulator/device is available, run instrumentation tests.

## Manual Acceptance

For behavior involving Android permissions, Accessibility, WhatsApp UI, alarms,
background execution, screen off, or lock screen, manual/device verification is
mandatory before declaring full completion.

Do not claim these are verified if they were not actually exercised.

## Reliability Test

A single successful message is not enough.

For release readiness, test repeated scheduled executions.

Target at least:

- 30 sequential scheduled sends;
- foreground;
- background;
- removed from Recents;
- screen off;
- WhatsApp already open;
- another chat open.

Record failures and investigate patterns.

## Do Not Fake Missing Results

If a test cannot be run, state:

```text
NOT TESTED
```

not:

```text
PASSED
```

If a value is unavailable, use `null` or `unavailable`.

Never invent successful runtime verification.

## Completion Report

Every completed substantial task must report:

- root cause;
- exact files/components changed;
- commands and results;
- runtime verification actually exercised on emulator/device;
- remaining limitations;
- anything still unsupported or unverified.

## Definition of Done

Do not mark a feature complete unless the user-visible workflow works end to
end.

For scheduling functionality this means, where applicable:

```text
Create
-> Save
-> persisted in Room
-> next occurrence computed
-> exact alarm armed
-> alarm fires
-> execution starts
-> correct WhatsApp target verified
-> message sent
-> result recorded
-> WhatsApp cleaned up
-> next occurrence scheduled
```

Anything less is partial completion.
