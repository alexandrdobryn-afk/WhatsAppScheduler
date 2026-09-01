# Permissions Audit

Manifest permissions after remediation:

- `android.permission.SCHEDULE_EXACT_ALARM`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.WAKE_LOCK`
- `android.permission.ACCESS_NETWORK_STATE`

Removed:

- `android.permission.USE_EXACT_ALARM`

Permissions not found:

- `android.permission.INTERNET`
- `android.permission.QUERY_ALL_PACKAGES`
- Location permissions
- Contacts permissions
- SMS permissions
- Camera permissions
- Microphone permissions
- Storage/media permissions

Package visibility after remediation:

- Query package: `com.whatsapp`

Permission assessment:

| Permission | Status | Assessment |
| --- | --- | --- |
| `SCHEDULE_EXACT_ALARM` | Present | Retained. Current scheduler uses exact alarms for user-created scheduled sends and checks `canScheduleExactAlarms()`. |
| `USE_EXACT_ALARM` | Removed | PASS. Restricted Play policy risk removed. |
| `RECEIVE_BOOT_COMPLETED` | Present | Restores schedule after reboot. |
| `POST_NOTIFICATIONS` | Present | Used for local status notifications. |
| `FOREGROUND_SERVICE` | Present | Plausible for execution/status support. Runtime behavior still needs device validation. |
| `WAKE_LOCK` | Present | Used with explicit timeout during scheduled send preparation. |
| `ACCESS_NETWORK_STATE` | Present | Used for connectivity precheck; no `INTERNET` permission is declared. |

Exact alarm remediation:

- Source manifest no longer declares `USE_EXACT_ALARM`.
- Merged release manifest no longer contains `USE_EXACT_ALARM`.
- Merged release manifest contains `SCHEDULE_EXACT_ALARM`.
- `AlarmScheduler` keeps the permission gate with `canScheduleExactAlarms()`.
- `ExactAlarmPermissionReceiver` listens for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` and enqueues the DB-backed reschedule/execution path.
- Boot, package-replaced, date/time/timezone changes still route through WorkManager and DB-backed rescheduling.

Runtime permission scenarios:

- Grant denied: NOT TESTED on device.
- Grant allowed: NOT TESTED on device.
- Revoke: NOT TESTED on device.
- `rescheduleNext`: statically present and build-verified; runtime NOT TESTED.
- Boot restore: statically present and build-verified; runtime NOT TESTED.
