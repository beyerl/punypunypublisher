# punypunypublisher

A tiny scheduled publishing app for Mastodon, inspired by [Tusky](https://tusky.app/). You write
a queue of short posts, configure weekly publish times, and the app drips one post out at each
slot — no server, no account beyond Mastodon, no cost.

## How it works

1. **Settings** — enter your Mastodon instance URL and a personal access token.
2. **Settings** — configure weekly publish times (weekday + UTC hour, any number of them). Each
   slot is shown as an easy-to-read label *and* its cron equivalent (e.g. `Every Friday 14:00 UTC`
   / `0 14 * * 5`).
3. **Compose** — edit the posts queue. **Every line is one Mastodon post.** At the next publishing
   slot the top line is published; on success it moves to the **Published** view.
4. Behind the scenes the queue and history are real markdown files in the app folder
   (`posts.md`, `published.md`); both can be exported from Settings.

### Scheduling

Publishing runs entirely on-device via Android **`WorkManager`** — there is no backend and no cost.
A periodic job wakes roughly every 15 minutes, and when the current UTC weekday+hour matches a
configured slot it publishes **one** post (a drip queue). Because the README's resolution is one
hour, Android's background-timing imprecision (Doze batching) doesn't matter: "publish sometime
within the slot hour" is exactly what WorkManager guarantees. The schedule survives reboots.

A **Publish next now** button in Settings triggers an immediate publish for manual testing.

## Getting a Mastodon access token

On your instance: **Preferences → Development → New application** (grant at least `write:statuses`),
create it, then copy the **access token**. Paste the instance URL and token into Settings.

## Building

The app is built with Gradle and the Android Gradle Plugin. GitHub Actions
(`.github/workflows/build.yml`) builds a debug APK on every push/PR and uploads it as the
`app-debug` artifact.

Locally (with a JDK 17 + Android SDK installed):

```bash
./gradlew test          # run unit tests
./gradlew assembleDebug # build app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug   # install onto a connected device/emulator
```

## Architecture

- `data/` — `MarkdownStore` (file IO for `posts.md` / `published.md`), `MarkdownFormat` (pure
  parse/serialize), `SettingsStore` (DataStore: credentials, publish times, last consumed slot),
  and the `PublishTime` / `PublishedEntry` models.
- `mastodon/MastodonClient` — posts a status via `POST /api/v1/statuses` (Ktor + OkHttp), using an
  `Idempotency-Key` so retries can't duplicate a post.
- `publish/PublishPlanner` — pure scheduling logic (slot id, due check, one-row-per-slot dedup).
- `scheduler/` — `PublishWorker` (the `CoroutineWorker` that orchestrates a publish) and
  `Scheduler` (enqueues the periodic job + the manual "publish now").
- `ui/` — Compose UI: Compose / Published / Settings tabs.

## Limitations (POC scope)

- One post per line — multi-line posts, media, threads, and OAuth login are out of scope.
- Posting requires the phone to be powered on (WorkManager runs on-device).
