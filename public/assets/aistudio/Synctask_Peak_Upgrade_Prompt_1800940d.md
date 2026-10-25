# SyncTask — Comprehensive Upgrade Prompt for Google AI Studio

Copy and paste the entire prompt below directly into **Google AI Studio** to refactor and upgrade your **SyncTask** applet into peak production form:

```text
Act as a Principal Frontend Architect and Product Designer. Refactor and upgrade this entire "SyncTask" application ("Smart task and event manager with offline Room sync, Google Calendar integration, focus timer, streaks, and milestone rewards") into peak production-grade form.

Preserve all existing functionality while systematically implementing the following 6 architectural, functional, and UI/UX upgrades across the codebase. Provide complete, working, self-contained code with zero placeholders or TODOs.

---

### 1. Offline-First Room Sync Architecture & Optimistic Mutation Queue
- **Storage Upgrade**: Replace brittle basic state or raw `localStorage` persistence with a structured, versioned offline storage layer (using `IndexedDB` via a lightweight wrapper or a robust local Room-style repository pattern with automatic fallback) storing `tasks`, `events`, `focusSessions`, `userStats`, and `syncQueue`.
- **Optimistic UI & Sync Queue**: Every create, update, toggle, or delete action must update the UI immediately (<16ms) and append a mutation record (`id`, `entityType`, `action`, `payload`, `timestamp`, `retryCount`, `syncStatus: 'synced' | 'pending' | 'conflict' | 'offline'`) to an offline sync queue.
- **Live Network & Sync Status Indicator**: Add an interactive header status pill showing real-time connectivity (`Online — Synced`, `Offline — Queued (N changes)`, `Syncing...`) using `navigator.onLine` and `online`/`offline` window events, plus a manual "Sync Now" button and a collapsible Sync Log drawer showing recent database transactions.
- **Data Portability**: Add "Export Backup (JSON)" and "Import / Restore JSON" actions in the settings/header menu so users never lose data.

---

### 2. Two-Way Google Calendar Integration & Unified Day Timeline
- **Unified Schedule & Time-Blocking View**: Build a dedicated split or toggleable "Timeline / Calendar" view alongside the task list showing a 24-hour (or 6 AM – 11 PM) daily/weekly schedule grid.
- **Time-Blocking Tasks**: Allow users to assign a scheduled start time and duration (e.g., 30m, 45m, 60m, 90m) to any task, or click "Schedule on Timeline" to place unscheduled tasks directly into open calendar slots.
- **Two-Way Sync & Conflict Resolution**:
  - Track `externalCalendarId`, `etag`, and `lastSyncedAt` on synced items.
  - Visually distinguish external Google Calendar events (with a Google Calendar badge and distinct accent border) from local SyncTask tasks.
  - Detect overlapping events/tasks and highlight scheduling conflicts with a warning badge and a 1-click "Auto-Resolve / Shift to Next Free Slot" action.
  - Support both real Google Calendar OAuth/API token input (optional in settings) AND a realistic interactive "Simulated Google Calendar Sync" mode pre-populated with realistic sample events so the feature works out-of-the-box without requiring API keys.

---

### 3. Drift-Free Background Focus Timer (Pomodoro + Deep Work)
- **Delta-Timestamp Engine**: Fix browser background-tab timer throttling by calculating remaining time from high-precision target timestamps (`targetEndTime - Date.now()`) rather than decrementing a counter inside `setInterval`.
- **Dynamic Document Title & Progress**: Update `document.title` in real time while the timer is active (e.g., `(18:42) 🎯 Focusing — SyncTask`) so users can see their countdown from any browser tab.
- **Task-Linked Sessions**: Let users select a specific active task to focus on. Automatically log completed focus minutes and Pomodoro cycles directly onto that task's record (displaying `Actual vs. Estimated Pomodoros`, e.g., `🍅 2/3`).
- **Audio & Notification Cues**: Synthesize clean, pleasant completion chimes and ambient white/brown noise options using the native browser **Web Audio API** (zero external MP3 dependencies that could fail to load) and trigger browser `Notification` alerts when a focus or break session ends.
- **Flexible Modes**: Include customizable Focus (25m / 50m / custom), Short Break (5m / 10m), and Long Break (15m / 30m) presets with an animated SVG circular progress ring.

---

### 4. Timezone-Safe Streak Engine & Milestone Rewards System
- **Local Date Normalization**: Calculate daily streaks using local timezone-normalized ISO date strings (`YYYY-MM-DD` via `Intl.DateTimeFormat` or local date parts) rather than raw UTC timestamps so late-night completions before midnight always count toward the correct local day.
- **Streak Freeze Protection**: Allow users to earn and spend "Streak Freeze" shields (automatically consumed if a day is missed) so a single off-day doesn't wipe out weeks of progress.
- **XP, Leveling & Milestone Badges**:
  - Award XP for completing tasks (scaled by priority: Low +10 XP, Medium +25 XP, High +50 XP, Urgent +100 XP) and finishing focus sessions (+30 XP per 25m session).
  - Display a persistent user level & XP progress bar in the sidebar/header.
  - Build a dedicated "Rewards & Milestones" showcase featuring unlockable achievement badges (e.g., *First Sync*, *7-Day Streak*, *Deep Work Master (10h focused)*, *Inbox Zero*, *Calendar Architect*), a 30-day GitHub-style activity heatmap, and celebratory canvas confetti micro-animations on milestone unlocks.

---

### 5. Smart Task Management & Keyboard-First Power UX
- **Command Bar & Natural Language Quick-Add (`Cmd/Ctrl + K`)**:
  - Implement a global keyboard shortcut (`Cmd+K` / `Ctrl+K` or `/`) and a prominent top Quick-Add bar.
  - Parse natural-language tokens in real time as the user types (with live pill previews):
    - Dates/times: `today`, `tomorrow`, `next monday`, `at 5pm`, `for 45m`
    - Priority tokens: `!low`, `!med`, `!high`, `!urgent` (or `p1`, `p2`, `p3`, `p4`)
    - Category/Tags: `#study`, `#work`, `#personal`, `#health`
- **Subtasks & Smart AI Breakdown**:
  - Support nested subtasks with an inline progress bar on each parent task card (e.g., `3/5 subtasks completed`).
  - Add a "⚡ Smart Breakdown" button on every task that automatically splits complex tasks into 3–5 actionable, time-estimated subtasks.
- **Filtering, Sorting & Views**: Provide instant filtering by status (`All`, `Today`, `Upcoming`, `Overdue`, `Completed`), tag, and priority, plus a toggle between **List View**, **Kanban Board View** (`To Do`, `In Progress`, `Done`), and **Calendar Timeline View**.

---

### 6. Peak UI/UX Polish, Accessibility & Responsive Design
- **Cohesive Visual System**: Use a refined dark/light theme toggle (defaulting to a sleek, high-contrast modern slate/indigo dark mode or crisp daylight mode), consistent 8px spatial grid, subtle glassmorphism on floating modals, and smooth micro-interactions.
- **Zero Layout Shift & Empty States**: Design helpful, motivating custom SVG empty states for empty task lists, completed days ("All caught up!"), and reward tiers.
- **Full Keyboard & ARIA Accessibility**: Ensure all interactive controls have proper `aria-label` attributes, focus rings, and keyboard shortcuts (`Space` to toggle task, `F` to start focus on selected task, `N` for new task).
```
