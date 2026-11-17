# AgainDue --- Android MVP Build Brief

> **Working title:** AgainDue (temporary; verify app-store, domain, and
> trademark availability before publishing)\
> **Product principle:** Remember the last time. Know when it's time
> again.\
> **Platform:** Android, native Kotlin\
> **Document purpose:** Implementation instructions for the coding
> agent. Treat this as the source of truth for the first MVP.

------------------------------------------------------------------------

## 1. Product definition

AgainDue is a private, offline-first Android app for tracking tasks that
repeat after an interval. The next due date is calculated from **when
the user actually completed the task**, not from the original scheduled
date.

Example:

1.  A user creates "Water plants" with a 7-day interval.
2.  The last-completed date is November 10.
3.  The next due date is November 17.
4.  The user completes the task on November 20.
5.  The next due date becomes November 27.

The core interaction is **Done**. The app should make it effortless to
answer:

-   What is due?
-   What did I last do?
-   When should I do it again?

This is not intended to become a general-purpose todo list, habit
tracker, calendar, or project manager.

## 2. Product positioning and differentiation

Similar completion-based reminder and maintenance apps already exist. Do
not claim that this is the first app to calculate recurrence from actual
completion.

The intended differentiation is a deliberately minimal experience:

-   Completion-first scheduling.
-   One-tap completion.
-   A clear urgency-sorted list.
-   Local storage and no account requirement.
-   No projects, priorities, streaks, social features, or complicated
    recurrence rules.
-   Reliable, understandable reminders.

The product should feel like a small utility, not another productivity
platform.

**Naming note:** "LastDone" appears to be used by an existing app with a
similar concept. Use "AgainDue" only as a temporary development name. Do
not finalize branding until availability checks are done.

## 3. MVP scope

### Must build

  -----------------------------------------------------------------------
  Feature                             Requirements
  ----------------------------------- -----------------------------------
  Home list                           Show all active items, sorted by
                                      urgency: most overdue first, due
                                      today, due soon, then not due

  Status                              Show "Overdue by N days", "Due
                                      today", or "Due in N days"

  Add item                            Name, interval value, interval
                                      unit, optional note, last-done date

  Edit item                           Rename, change interval, update
                                      last-done date, edit note

  Done                                One tap records completion and
                                      recalculates the next due date from
                                      the actual completion date

  Undo                                Allow recovery from an accidental
                                      completion; restore the previous
                                      completion state

  Archive                             Hide an item without permanently
                                      deleting it

  Delete                              Permanently remove an item after
                                      confirmation

  Notifications                       Schedule a local reminder for each
                                      active item

  Snooze                              Offer "Snooze 1 day" in the app and
                                      notification

  Backup                              Export and import JSON through
                                      Android's Storage Access Framework

  Starter suggestions                 Offer optional sample items on
                                      first launch; do not force users to
                                      keep them

  Settings                            Global reminder time, notification
                                      permission status, backup/import,
                                      about
  -----------------------------------------------------------------------

### Explicitly out of scope for MVP

-   Accounts, authentication, cloud sync, backend, or multi-user
    support.
-   Google Calendar integration.
-   Categories, tags, projects, priorities, or task dependencies.
-   Streaks, charts, analytics, leaderboards, or gamification.
-   Social features.
-   Home-screen widgets.
-   Complex recurrence rules such as weekdays, weekdays-only, or "last
    Friday of every month".
-   Multiple reminder times per item.
-   A web application or iOS application.

Do not add features outside the MVP unless the user explicitly changes
this scope.

## 4. Core product rules

### 4.1 Interval calculation

Each item has:

-   A name.
-   A positive integer interval.
-   An interval unit: days, weeks, or months.
-   A last-completed date.
-   A derived next-due date.

Calculation:

-   Days: `lastDoneAt.plusDays(intervalValue)`
-   Weeks: `lastDoneAt.plusWeeks(intervalValue)`
-   Months: `lastDoneAt.plusMonths(intervalValue)`

Never approximate a month as 30 days. Use `java.time.LocalDate` and its
calendar-aware month behavior.

Examples:

-   January 31 + 1 month = February 28 (or February 29 in a leap year).
-   March 5 + 1 month = April 5.
-   If a user completes a monthly task early or late, calculate from the
    actual completion date.

### 4.2 Completion behavior

-   Completing early is allowed.
-   Completing on the due date is allowed.
-   Completing late is allowed.
-   Every completion sets `lastDoneAt` to the actual local date of
    completion.
-   Recalculate `nextDueAt` immediately.
-   Do not preserve the original schedule after completion.
-   A completion must be persisted before its next reminder is
    scheduled.
-   Undo must restore the previous completion date and derived due date,
    then reschedule notifications.

### 4.3 Editing behavior

-   Changing an interval recalculates `nextDueAt` from the existing
    `lastDoneAt`.
-   Changing `lastDoneAt` recalculates `nextDueAt`.
-   Renaming does not change dates.
-   Archiving cancels its pending notification.
-   Restoring an archived item schedules its next reminder if
    notifications are enabled.
-   Deleting an item cancels its pending notification and removes its
    stored data.

### 4.4 Date and time semantics

Use `LocalDate` for task dates. The task itself does not need a
time-of-day.

Use one global notification time setting, defaulting to 09:00 local
time. Convert each item's `nextDueAt` plus this setting into the
device's current local date-time when scheduling.

On date, time, or timezone changes, recalculate and reschedule alarms.
Document the behavior clearly.

### 4.5 Status and sorting

Status is derived from `nextDueAt` compared with today's `LocalDate`:

-   `nextDueAt < today`: overdue.
-   `nextDueAt == today`: due today.
-   `nextDueAt > today`: due in N days.

Sort active items by: 1. Most overdue first (earliest due date). 2. Due
today. 3. Upcoming items in ascending due-date order.

Use a deterministic secondary sort, such as item name, so the list does
not jump unpredictably.

## 5. Screens and UX

Keep the MVP to three main screens.

### Home

Each item row/card should show:

-   Item name.
-   Last done date.
-   Interval (for example, "Every 7 days").
-   Status label.
-   A prominent Done action.
-   A secondary action for snoozing or opening edit.

The list must make overdue items easy to identify without relying on
color alone. Provide an empty state that explains how to add the first
item.

A floating action button opens Add Item. Settings are available from the
top app bar overflow/menu.

### Add / Edit Item

Fields:

-   Name (required).
-   Interval value (required positive integer).
-   Interval unit (days, weeks, months).
-   Last done date (defaults to today for a new item).
-   Note (optional, short text).

Validate empty names, zero/negative intervals, and invalid input.
Explain validation inline.

### Settings

-   Global notification time.
-   Notification permission status and a way to enable notifications.
-   Export JSON.
-   Import JSON.
-   About and privacy statement.

Do not create a separate history screen in MVP. Keep enough completion
data to support undo and basic recovery.

## 6. Data model

Use Room with SQLite. A suggested starting model:

``` kotlin
enum class IntervalUnit {
    DAY, WEEK, MONTH
}

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val intervalValue: Int,
    val intervalUnit: IntervalUnit,

    val lastDoneAt: LocalDate,
    val nextDueAt: LocalDate,

    val note: String? = null,
    val archived: Boolean = false,

    val createdAt: Instant,
    val updatedAt: Instant
)
```

Store enum/date/time values using explicit Room `TypeConverter`s. Use a
consistent, documented serialization format.

For undo, add a small completion record or equivalent persisted
previous-state snapshot. Prefer a `Completion` entity with an item ID,
completion timestamp, and previous last-done date. Keep the design
simple, but do not implement undo as a UI-only trick that is lost on
process death.

Suggested completion model:

``` kotlin
@Entity(tableName = "completions")
data class Completion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val itemId: Long,
    val completedAt: Instant,
    val previousLastDoneAt: LocalDate
)
```

Use a Room transaction when recording a completion and updating the
item. Define how long undo remains available; for MVP, the most recent
completion can be undone while the user remains in the app, and its
record should remain available for safe recovery.

`nextDueAt` is derived data. Recompute it from `lastDoneAt`,
`intervalValue`, and `intervalUnit` when an item is created, edited,
completed, imported, or when app data is reconciled. Do not allow stale
stored values to become the source of truth.

## 7. Architecture and technology

Use native Android development.

  -----------------------------------------------------------------------
  Layer                               Choice
  ----------------------------------- -----------------------------------
  Language                            Kotlin

  UI                                  Jetpack Compose + Material 3

  Architecture                        MVVM, ViewModel, StateFlow

  Persistence                         Room / SQLite

  Date/time                           `java.time`

  Notifications                       AlarmManager + BroadcastReceiver +
                                      NotificationCompat

  Serialization                       kotlinx.serialization

  Dependency injection                Manual for MVP

  Backup                              Storage Access Framework (SAF)

  Minimum SDK                         Choose API 26 or newer; document
                                      the final choice

  Target SDK                          Use the latest stable target
                                      required by current Android/Google
                                      Play policy at implementation time
  -----------------------------------------------------------------------

Keep domain logic independent of Compose and Android framework classes
so it can be unit-tested.

Suggested package structure:

``` text
app/
├── data/
│   ├── local/
│   │   ├── Item.kt
│   │   ├── Completion.kt
│   │   ├── ItemDao.kt
│   │   ├── CompletionDao.kt
│   │   ├── AppDatabase.kt
│   │   └── Converters.kt
│   └── repository/
│       └── ItemRepository.kt
├── domain/
│   ├── NextDueCalculator.kt
│   ├── ItemStatus.kt
│   ├── ItemSorter.kt
│   └── CompletionUseCases.kt
├── notification/
│   ├── AlarmScheduler.kt
│   ├── AlarmReceiver.kt
│   ├── BootReceiver.kt
│   └── NotificationHelper.kt
├── ui/
│   ├── home/
│   ├── edit/
│   ├── settings/
│   └── components/
└── MainActivity.kt
```

Adjust names to match the generated project conventions, but preserve
the separation of UI, domain, data, and notification responsibilities.

## 8. Notification implementation

Notifications are important, but exact timing is not essential for every
task. Do not make exact alarms the default.

### Recommended behavior

-   Use inexact alarms by default for ordinary reminders.
-   Consider exact alarms only if the user explicitly opts into precise
    reminder timing and the current Android version/policy permits it.
-   Check exact-alarm capability before attempting to schedule an exact
    alarm.
-   Provide a graceful fallback to an inexact alarm if exact access is
    unavailable.
-   Do not block normal app use if notification or exact-alarm
    permissions are denied.

### Required cases to handle

-   Android 13+ notification runtime permission.
-   Exact-alarm access where applicable.
-   Device reboot: restore scheduled reminders.
-   App replacement/update: reconcile scheduled reminders.
-   Date/time/timezone change: recalculate and reschedule.
-   Item completed, edited, archived, deleted, imported, or restored.
-   Notification tap: open the relevant item.
-   Notification actions: Done and Snooze 1 day.
-   Duplicate alarms: use stable per-item request codes/identifiers and
    cancel stale alarms before rescheduling.

Ask for notification permission when the user enables reminders or
reaches a clear reminder-related action, not immediately on first launch
without context.

Do not promise that every Android device will deliver reminders at an
exact minute. Battery restrictions and OEM behavior can affect delivery.
Explain any relevant limitations in Settings.

On app launch, reconcile the stored items with scheduled reminders.
Recompute due dates from source fields and replace stale alarms.

## 9. Backup and restore

Export all necessary item data, settings, and completion state to JSON
using SAF. Do not require broad storage permissions.

Import must:

-   Validate the file and schema version.
-   Handle malformed or unsupported files gracefully.
-   Ask for confirmation before replacing existing data.
-   Prevent duplicate or invalid records.
-   Recalculate all derived due dates after import.
-   Reschedule reminders after a successful import.

Include a schema version in the JSON format so future app versions can
migrate backups safely.

Do not claim that Android device backup replaces user-controlled
export/import.

## 10. Testing and acceptance criteria

Write unit tests for domain logic before wiring all UI behavior.

### Due-date calculation tests

-   7 days after a known date.
-   2 weeks after a known date.
-   1 month after January 31 in a non-leap year.
-   1 month after January 31 in a leap year.
-   Multiple months from a month-end date.
-   Early completion shifts the next due date forward from today.
-   Late completion shifts the next due date forward from the actual
    completion date.
-   Editing an interval recalculates from the last-completed date.

### Status and sorting tests

-   Yesterday's due date is overdue by one day.
-   Today's due date is due today.
-   Tomorrow's due date is due in one day.
-   Most overdue item sorts first.
-   Upcoming items sort in ascending due-date order.
-   Archived items do not appear in the active list.

### Completion and persistence tests

-   Done updates the last-completed date.
-   Done updates the next due date.
-   Completion and item update happen atomically.
-   Undo restores the previous date and due date.
-   Editing and archiving affect the correct item only.
-   Relaunching the app preserves state.

### Notification tests

-   A due item receives a scheduled reminder when permission is
    available.
-   Denied notification permission does not crash the app.
-   Exact-alarm access denial falls back gracefully.
-   Completing an item cancels/replaces its old reminder.
-   Archiving or deleting cancels its reminder.
-   Snooze schedules a one-day-later reminder.
-   Reboot reconciliation restores pending reminders.
-   Timezone/date changes trigger rescheduling.

### Backup tests

-   Export produces valid versioned JSON.
-   Import restores items and settings.
-   Invalid JSON is rejected with a useful error.
-   Import recalculates derived dates.
-   Import does not leave duplicate alarms.

## 11. Implementation sequence

Build in small, verifiable milestones. Do not attempt all features in
one large code-generation pass.

### Milestone 0 --- Project setup

-   Inspect the existing repository before changing files.
-   Confirm Android Studio/Gradle/Kotlin versions and project structure.
-   Create or verify the Compose application.
-   Set up Material 3, Room, serialization, and test dependencies.
-   Build and run the empty app.

**Done when:** the app builds and launches on an emulator/device.

### Milestone 1 --- Domain logic

-   Implement interval units and `NextDueCalculator`.
-   Implement status calculation.
-   Implement urgency sorting.
-   Add unit tests for month-end, leap-year, early, and late completion.

**Done when:** all domain tests pass without depending on Android UI.

### Milestone 2 --- Persistence

-   Implement Room entities, converters, DAOs, and database.
-   Implement repository methods.
-   Implement completion transaction and undo state.
-   Add sample data only as optional starter suggestions.

**Done when:** create, edit, complete, undo, archive, and delete survive
app restarts.

### Milestone 3 --- Core UI

-   Build Home.
-   Build Add/Edit.
-   Build Settings shell.
-   Connect ViewModels and StateFlows.
-   Implement validation, empty states, and loading/error states.

**Done when:** a user can manage recurring items without notifications.

### Milestone 4 --- Notifications

-   Implement alarm scheduling and cancellation.
-   Implement notification channels.
-   Implement notification actions.
-   Implement permission education and runtime permission flow.
-   Implement boot/update/time-change reconciliation.
-   Test on a real device where possible.

**Done when:** reminders are correctly scheduled, replaced, and
restored, with graceful permission fallbacks.

### Milestone 5 --- Backup and polish

-   Implement versioned JSON export/import through SAF.
-   Add confirmation and error handling.
-   Verify accessibility, touch targets, and text scaling.
-   Test app relaunch and backup restoration.
-   Write a concise privacy statement.

**Done when:** the app is usable offline and user data can be exported
and restored.

### Milestone 6 --- Personal validation

Use the MVP yourself for at least 14 days before adding new features.
Ask a few other people to try it if possible.

Record actual friction. Fix bugs and confusing interactions; do not add
features just because they sound useful.

## 12. Agent operating instructions

Follow these rules throughout implementation:

1.  **Inspect first.** Read the repository, existing README, Gradle
    files, and project conventions before editing.
2.  **Plan one milestone at a time.** State the intended files and
    changes briefly, then implement the smallest coherent increment.
3.  **Build and test after each milestone.** Do not claim success unless
    the relevant build/tests actually pass.
4.  **Keep domain logic testable.** Do not put interval calculations
    inside Composables or notification receivers.
5.  **Avoid unnecessary dependencies.** Prefer Android/Kotlin standard
    libraries and established Jetpack components.
6.  **Handle permissions as optional capabilities.** The app must remain
    usable when notifications are denied.
7.  **Do not silently change product behavior.** Ask before expanding
    scope or changing the completion-first rule.
8.  **Do not add a backend.** The MVP is local-first and single-user.
9.  **Do not implement Calendar integration in MVP.**
10. **Keep code readable.** Prefer straightforward Kotlin over clever
    abstractions.
11. **Document non-obvious Android behavior.** Especially alarm
    permission behavior, notification reliability, and backup format.
12. **Report honestly.** At the end of each milestone, list files
    changed, tests/build commands run, results, and known issues.

If a technical detail depends on current Android SDK or Play policy,
verify it against current official Android documentation rather than
relying on an outdated assumption.

## 13. MVP success criteria

After 14 days of real use, evaluate:

-   Did the user create at least 5--10 recurring items?
-   Did they use Done repeatedly?
-   Did reminders arrive reliably enough to be useful?
-   Did the app reduce the need to remember maintenance intervals
    manually?
-   Was the app simpler than their previous method?
-   Would they be annoyed if the app disappeared?

If the app is not being used, investigate the workflow and problem
before adding features.

## 14. Later possibilities --- not commitments

Only consider these after MVP validation:

-   Lightweight completion history ("last 5 completions").
-   Home-screen widget.
-   Quick-add from a notification.
-   Optional calendar export, if users explicitly request it.
-   Per-item notification settings, if the global setting proves
    insufficient.

Do not start these while the MVP is incomplete.

------------------------------------------------------------------------

## Final instruction

Build the smallest reliable version of this product. The value is not
the number of screens or features. The value is that a person can record
when they actually did something and trust the app to tell them when it
is due again.
