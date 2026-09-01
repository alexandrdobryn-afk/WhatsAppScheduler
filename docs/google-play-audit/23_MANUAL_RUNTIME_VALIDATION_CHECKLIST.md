# Manual Runtime Validation Checklist

Use the rebuilt API 36 release artifact. Record logcat for every failure.

## Install And Permissions

- [ ] Clean install.
- [ ] First launch does not crash.
- [ ] Onboarding completes.
- [ ] Prominent Accessibility disclosure appears before Android settings.
- [ ] Accessibility enabled.
- [ ] Accessibility connected after returning to app.
- [ ] Exact alarm permission denied path shows blocked scheduling state.
- [ ] Exact alarm permission allowed path calls DB-backed reschedule.
- [ ] Exact alarm permission revoke cancels/prevents future exact schedules.
- [ ] Re-grant exact alarm permission triggers `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` and reschedules.

## Schedule Types

- [ ] Weekly Sunday.
- [ ] Weekly weekends.
- [ ] Weekly custom days.
- [ ] Multiple times on one rule.
- [ ] Specific nearest date.
- [ ] Specific date executes once and does not reschedule after final occurrence.
- [ ] Multiple dates choose correct next occurrence.

After every save:

- [ ] Room has the rule/times/dates.
- [ ] Home shows rule.
- [ ] Next execution is correct.
- [ ] AlarmScheduler arms expected next alarm.

## Accessibility

- [ ] OFF state is distinct from enabled-not-connected.
- [ ] ON connected state is logged.
- [ ] ON after process recreation.
- [ ] ON after app background.
- [ ] ON after Activity destroyed.
- [ ] ON after repeated sends.
- [ ] Disconnect count recorded.

## WhatsApp Send

Use normal consumer WhatsApp group, not Community, not WhatsApp Business.

- [ ] Dry Run.
- [ ] Test Send.
- [ ] Scheduled Send.
- [ ] WhatsApp closed.
- [ ] WhatsApp open on chat list.
- [ ] Target chat already open.
- [ ] Another chat open.
- [ ] Actual target verified before Send.
- [ ] After success, WhatsApp returns Home/background; no force-stop.

## Background And Screen

- [ ] WA Schedule foreground.
- [ ] WA Schedule background.
- [ ] Activity destroyed.
- [ ] Removed from Recents.
- [ ] Screen ON.
- [ ] Screen OFF with non-secure keyguard: wake, dismiss, send, Home, release.
- [ ] Secure lock: do not bypass, record `DEVICE_SECURE_LOCKED`, retry within allowed delay.

## Migration And Reliability

- [ ] Install previous v2/schema build and create saved rule.
- [ ] Update to current API 36 build.
- [ ] Old rules preserved.
- [ ] New schema/tables intact; no destructive migration.
- [ ] Run at least 30 scheduled sends with mixed foreground/background/WhatsApp/screen states.
- [ ] Record total, sent, failed, skipped, duplicates, accessibility disconnects.
