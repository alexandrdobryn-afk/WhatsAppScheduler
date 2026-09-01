# Architecture

WA Schedule is a local Android app built with Kotlin, Jetpack Compose, Room,
Hilt, AlarmManager, WorkManager, and Android AccessibilityService.

Product-release requirements are tracked in
[PRODUCT_RELEASE_SPEC.md](PRODUCT_RELEASE_SPEC.md). Architecture changes that
touch scheduling, Accessibility, privacy, release, or Google Play readiness
must preserve that specification unless a later task explicitly supersedes it.

```text
UI (Compose) -> ViewModel -> Domain use cases -> Repository -> Room
                                                     |
                                                     v
                              Scheduler (AlarmManager, DB = source of truth)
                                                     |
                                                     v
                                  AutomationEngine (prechecks, dedup, retry)
                                                     |
                                                     v
                         WhatsAppAccessibilityService -> WhatsAppUiAdapter
```

## Modules

- `core/scheduler` - `AlarmScheduler`, exact alarms, timezone-aware schedule
  calculation, rescheduling after reboot, app update, and time changes.
- `core/automation` - `AutomationEngine`, `ExecutionWorker`, `RetryWorker`,
  prechecks, claim/dedup, retry policy, execution logging, and notifications.
- `core/accessibility` - `WhatsAppAccessibilityService`,
  `AutomationStateMachine`, `WhatsAppUiAdapter`, and WhatsApp-specific selectors.
- `core/permissions` - Android permission and system setting checks.
- `data/` - Room entities, DAOs, migrations, repositories, and DataStore
  settings.
- `domain/` - app models, repository interfaces, schedule calculation use cases.
- `feature/` - Compose screens for Home, Rule Editor, History, Settings,
  Diagnostics, and Onboarding.
- `service/` - Alarm, boot/package-replaced, and time-change receivers.

## Scheduling

The database is the source of truth. Any meaningful event can recompute the next
alarm:

- rule create/update/delete;
- app start;
- boot completed;
- app package replaced;
- device time/date/timezone changed;
- execution result.

`ComputeNextOccurrenceUseCase` calculates the next future occurrence. Rules have
a start date, one or more times, selected weekdays, and an allowed lateness
window. `CollectDueOccurrencesUseCase` collects occurrences that should run now
and skips rules before their start date.

## Execution

When an alarm fires, `ExecutionWorker` collects due occurrences and asks
`AutomationEngine` to execute them. The engine performs prechecks, claims the
execution to prevent duplicates, invokes the Accessibility automation, records
the result, schedules retries when allowed, and reschedules the next alarm.

Execution IDs are deterministic per rule and scheduled time, which prevents
duplicate sends for the same occurrence.

## Accessibility

The service is configured for:

```text
com.whatsapp
```

It uses a state machine:

```text
LAUNCHING -> FINDING_CHAT -> VERIFYING_CHAT -> FINDING_INPUT ->
SETTING_TEXT -> FINDING_SEND_BUTTON -> SENDING -> VERIFYING -> SUCCESS/FAILED
```

WhatsApp UI selectors are isolated in `WhatsAppUiAdapterImpl` so changes in the
WhatsApp UI can be repaired in one place.

## Storage

Room stores:

- rules;
- rule times;
- execution logs.

DataStore stores:

- global enabled flag;
- theme;
- interface language;
- notification settings;
- retry count;
- scheduler timezone mode.

The app does not store arbitrary WhatsApp chat content.
