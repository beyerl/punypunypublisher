#!/usr/bin/env python3
"""Publish due posts from posts.json to Mastodon, then mark them published and push.

Run by .github/workflows/publish.yml once a day. A post is due when
`published == false and date <= today` (today in PUBLISH_TZ). Due posts go out oldest first,
each with `Idempotency-Key: post-<id>`, so a crash between posting and committing can't
double-post on the retry. Results are committed with a fetch/reset/re-apply loop, so a
concurrent save from the app never gets overwritten.

Environment:
  MASTODON_INSTANCE  e.g. https://mastodon.social      (required)
  MASTODON_TOKEN     access token with write:statuses  (required)
  POSTS_FILE         default posts.json
  PUBLISH_TZ         default UTC (must match PostRules.ZONE in the app)
  GITHUB_REF_NAME    branch to push to (set by Actions)
  DRY_RUN=1          list due posts, don't post or commit
"""
import datetime
import json
import os
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import zoneinfo

POSTS_FILE = os.environ.get("POSTS_FILE", "posts.json")


def load(path=POSTS_FILE):
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8") as f:
        text = f.read()
    return json.loads(text) if text.strip() else []


def save(posts, path=POSTS_FILE):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(posts, f, indent=2, ensure_ascii=False)
        f.write("\n")


def due_posts(posts, today):
    """Unpublished posts dated today or earlier, oldest first."""
    due = [p for p in posts if not p.get("published") and p["date"] <= today.isoformat()]
    return sorted(due, key=lambda p: (p["date"], p["id"]))


def mark_published(posts, results):
    """Set published/status fields for every id in results ({id: status dict})."""
    for p in posts:
        status = results.get(p["id"])
        if status is not None:
            p["published"] = True
            p["status_id"] = str(status.get("id", ""))
            if status.get("url") or status.get("uri"):
                p["status_url"] = status.get("url") or status.get("uri")
    return posts


def normalize_instance(raw):
    raw = raw.strip().rstrip("/")
    return raw if raw.startswith(("http://", "https://")) else "https://" + raw


def post_status(instance, token, post):
    req = urllib.request.Request(
        normalize_instance(instance) + "/api/v1/statuses",
        data=urllib.parse.urlencode({"status": post["text"]}).encode(),
        method="POST",
        headers={
            "Authorization": "Bearer " + token,
            "Idempotency-Key": "post-%d" % post["id"],
            "User-Agent": "punypunypublisher",
        },
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)


def git(*args):
    return subprocess.run(["git", *args], check=False).returncode == 0


def commit_and_push(results, branch, attempts=5):
    ids = ", ".join("#%d" % i for i in sorted(results))
    for attempt in range(attempts):
        save(mark_published(load(), results))
        if git("diff", "--quiet", "--", POSTS_FILE):
            return
        git("add", POSTS_FILE)
        if not git("commit", "-m", "Mark posts published: " + ids):
            raise SystemExit("git commit failed")
        if git("push", "origin", "HEAD:" + branch):
            return
        # The app (or someone) pushed meanwhile: take theirs and re-apply our markers.
        print("Push rejected, retrying on top of origin/%s (attempt %d)" % (branch, attempt + 1))
        git("fetch", "origin", branch)
        git("reset", "--hard", "origin/" + branch)
    raise SystemExit("Could not push published markers after %d attempts" % attempts)


def main():
    tz = zoneinfo.ZoneInfo(os.environ.get("PUBLISH_TZ", "UTC"))
    today = datetime.datetime.now(tz).date()
    due = due_posts(load(), today)
    print("Today is %s; %d post(s) due." % (today, len(due)))
    if not due or os.environ.get("DRY_RUN"):
        for p in due:
            print("  would publish #%d (%s)" % (p["id"], p["date"]))
        return 0

    instance = os.environ.get("MASTODON_INSTANCE", "")
    token = os.environ.get("MASTODON_TOKEN", "")
    if not instance or not token:
        print("MASTODON_INSTANCE and MASTODON_TOKEN secrets must be set.", file=sys.stderr)
        return 1

    results, failed = {}, 0
    for p in due:
        if not p.get("text", "").strip():
            print("Skipping #%d: empty text" % p["id"], file=sys.stderr)
            failed += 1
            continue
        try:
            status = post_status(instance, token, p)
        except urllib.error.HTTPError as e:
            print("Failed #%d: HTTP %d %s" % (p["id"], e.code, e.read().decode(errors="replace")[:500]), file=sys.stderr)
            failed += 1
            continue
        except (urllib.error.URLError, TimeoutError) as e:
            print("Failed #%d: %s" % (p["id"], e), file=sys.stderr)
            failed += 1
            continue
        results[p["id"]] = status
        print("Published #%d -> %s" % (p["id"], status.get("url") or status.get("uri")))

    if results:
        commit_and_push(results, os.environ.get("GITHUB_REF_NAME", "main"))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
