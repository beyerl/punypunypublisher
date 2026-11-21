# punypunypublisher

A tiny scheduled publishing app for Mastodon, inspired by [Tusky](https://tusky.app/). You write
a queue of posts on your phone; one goes out each week. No server, no cost.

## How it works

```
GitHub repo (this one)
 ├─ posts.json                      ← single source of truth
 └─ .github/workflows/publish.yml   ← daily publisher (scripts/publish.py)

Android app:    GET posts.json → edit → PUT posts.json (sha-checked, retried on 409)
GitHub Action:  daily 09:17 UTC → post every due, unpublished post to Mastodon
                (Idempotency-Key: post-<id>) → set published: true → commit
```

- **Upcoming** tab: posts not yet published. The top one goes out next. Tap **+** to add one
  and tap a post to edit or delete it. Posts can span several lines.
- **Published** tab: what the workflow has actually posted, newest first, read-only.
- **Settings**: the GitHub repo and token, and the character limit.

The app shows only the text. Each post in `posts.json` also has an `id` and a `date`, which
the app handles:

| Field | Rule |
|-------|------|
| `id` | `max(id) + 1`, never reused. |
| `date` (`YYYY-MM-DD`, UTC) | New post: latest date in the file (published posts included) + 7 days, never earlier than today. Editing keeps the date. Dates are unique. |
| `published`, `status_id`, `status_url` | Set only by the workflow after a successful post. |

To reschedule, edit `date` in `posts.json` on GitHub directly. The workflow posts everything
with `date <= today` that isn't published yet, so a missed run catches up the next day.

Saving needs a connection. Offline saves fail and the editor keeps your text.

> **Note:** this repo is public, so drafts in `posts.json` are visible to anyone. To keep them
> private, put `posts.json`, `scripts/publish.py` and `.github/workflows/publish.yml` in a private
> repo and point the app's Settings at it.

## Setup

### 1. Mastodon token → GitHub Actions secrets

1. On your instance: **Preferences → Development → New application**. Give it the scope
   `write:statuses` only (untick the rest), then submit. Open the app and copy **Your access token**.
2. On GitHub: **this repo → Settings → Secrets and variables → Actions → New repository secret**:
   - `MASTODON_INSTANCE` = `https://your.instance`
   - `MASTODON_TOKEN` = the access token
3. Test it: **Actions → Publish → Run workflow**. It logs `0 post(s) due` when nothing is due.

### 2. GitHub token → app

1. GitHub: **Settings → Developer settings → Personal access tokens → Fine-grained tokens →
   Generate new token**.
2. Resource owner: you. Repository access: **Only select repositories**, then this repo.
3. Permissions → Repository permissions → **Contents: Read and write**. Nothing else is needed
   (Metadata: read-only is added automatically).
4. Choose an expiration and generate the token. Paste it into the app under **Settings**, check
   owner/repo/branch, and tap **Save and connect**. The token is encrypted with a key in the
   Android Keystore. When it expires, the app reports that it's invalid; generate a new one.

### Upgrading from 0.1 (on-device publishing)

0.2 cancels the old on-device job and removes the old Mastodon token from the phone. If the
old `posts.md` / `published.md` still have content, **Settings → Import old posts** moves them
into `posts.json`: queued lines become weekly upcoming posts and history becomes published posts.

## Building

GitHub Actions (`.github/workflows/build.yml`) runs the tests and builds a debug APK on every
push. The APK is uploaded as the `app-debug` artifact. Pushes that change only `posts.json` are
skipped.

Locally (JDK 17 + Android SDK):

```bash
./gradlew test                                  # app unit tests
python3 -m unittest discover -s scripts         # publisher tests
./gradlew assembleDebug                         # app/build/outputs/apk/debug/app-debug.apk
DRY_RUN=1 python3 scripts/publish.py            # show which posts are due
```

## Architecture

- `data/`: the `Post` model, `PostsJson` (same layout as Python's `json.dump(indent=2)`),
  `SettingsStore` (DataStore) with `TokenCipher` (Keystore AES-GCM), and the legacy
  `MarkdownStore`/`MarkdownFormat`, kept only for the import.
- `posts/`: `PostRules` (ids, dates, validation, read-only published posts) and `LegacyImport`.
- `github/`: `GitHubClient` (Contents API GET/PUT) and `PostRepository` (on 409: re-fetch, re-apply the edit, retry).
- `ui/`: Compose UI with Upcoming, Published and Settings tabs, plus the post editor.
- `scripts/publish.py`: the Actions publisher (stdlib only).
