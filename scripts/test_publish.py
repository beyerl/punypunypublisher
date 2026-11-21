import datetime
import unittest

import publish


class PublishTest(unittest.TestCase):
    posts = [
        {"id": 1, "date": "2026-11-01", "text": "old", "published": True},
        {"id": 3, "date": "2026-11-21", "text": "today", "published": False},
        {"id": 2, "date": "2026-11-14", "text": "missed", "published": False},
        {"id": 4, "date": "2026-11-28", "text": "future", "published": False},
    ]

    def test_due_posts_are_unpublished_past_or_today_oldest_first(self):
        due = publish.due_posts(self.posts, datetime.date(2026, 11, 21))
        self.assertEqual([2, 3], [p["id"] for p in due])

    def test_mark_published_sets_flag_and_status(self):
        posts = [dict(p) for p in self.posts]
        publish.mark_published(posts, {2: {"id": "99", "url": "https://m/@me/99"}})
        p2 = next(p for p in posts if p["id"] == 2)
        self.assertTrue(p2["published"])
        self.assertEqual("99", p2["status_id"])
        self.assertEqual("https://m/@me/99", p2["status_url"])
        self.assertFalse(next(p for p in posts if p["id"] == 3)["published"])

    def test_normalize_instance(self):
        self.assertEqual("https://mastodon.social", publish.normalize_instance("mastodon.social/"))
        self.assertEqual("http://localhost:3000", publish.normalize_instance("http://localhost:3000"))


if __name__ == "__main__":
    unittest.main()
