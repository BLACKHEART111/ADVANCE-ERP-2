import os
from contextlib import closing
from pathlib import Path
import sqlite3
import tempfile
import unittest
from unittest.mock import patch

from prepare_tuktak import identity, minor, normalized, phone_key, snapshot, verify_preview


class MigrationSafetyTests(unittest.TestCase):
    def test_money_exact_and_checked(self):
        self.assertEqual(minor(267970, 100), 26797000)
        self.assertEqual(minor(-400, 100), -40000)
        for value in (1.25, "100", None):
            with self.assertRaises(ValueError):
                minor(value, 100)
        with self.assertRaises(ValueError):
            minor(2**63, 100)

    def test_id_namespace_distinguishes_tables_and_is_repeatable(self):
        self.assertEqual(identity("products", "abc"), identity("products", "abc"))
        self.assertNotEqual(identity("products", "abc"), identity("customers", "abc"))

    def test_duplicate_keys_normalize_local_phones_and_names(self):
        self.assertEqual(phone_key("+880 1712-345678"), phone_key("01712345678"))
        self.assertEqual(phone_key("00880 1712-345678"), phone_key("০১৭১২৩৪৫৬৭৮"))
        self.assertEqual(normalized("  HEIGHT   INSOLE "), "height insole")

    def test_continuously_changing_snapshot_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=os.environ["TMPDIR"]) as tmp:
            with patch("prepare_tuktak.adb", side_effect=[str(i).encode() for i in range(20)]):
                with self.assertRaisesRegex(RuntimeError, "changing"):
                    snapshot("package", "database", Path(tmp) / "output.db")
            self.assertFalse((Path(tmp) / "output.db").exists())

    def test_wal_snapshot_keeps_committed_recent_rows(self):
        with tempfile.TemporaryDirectory(dir=os.environ["TMPDIR"]) as tmp:
            path = Path(tmp) / "live.db"
            live = sqlite3.connect(path)
            live.execute("PRAGMA journal_mode=WAL")
            live.execute("CREATE TABLE events(id INTEGER PRIMARY KEY)")
            live.execute("PRAGMA wal_checkpoint(TRUNCATE)")
            live.execute("INSERT INTO events VALUES(7)")
            live.commit()
            bundle = {"databases/live.db": path.read_bytes(),
                      "databases/live.db-wal": Path(str(path) + "-wal").read_bytes()}
            with patch("prepare_tuktak.adb", side_effect=lambda *args: bundle[args[-1]]):
                snapshot("package", "live.db", Path(tmp) / "snapshot.db")
            with closing(sqlite3.connect(Path(tmp) / "snapshot.db")) as restored:
                self.assertEqual(restored.execute("SELECT id FROM events").fetchall(), [(7,)])
            live.close()

    def test_baseline_change_and_history_additions_are_rejected(self):
        with tempfile.TemporaryDirectory(dir=os.environ["TMPDIR"]) as tmp:
            baseline, preview = Path(tmp) / "before.db", Path(tmp) / "after.db"
            for path in (baseline, preview):
                with closing(sqlite3.connect(path)) as db, db:
                    db.execute("CREATE TABLE history(id TEXT PRIMARY KEY, amount INTEGER)")
                    db.execute("INSERT INTO history VALUES('original',100)")
            self.assertTrue(verify_preview(baseline, preview, [])["existing_rows_unchanged"])
            with closing(sqlite3.connect(preview)) as db, db:
                db.execute("UPDATE history SET amount=200")
            with self.assertRaisesRegex(ValueError, "preservation"):
                verify_preview(baseline, preview, [])
            with closing(sqlite3.connect(preview)) as db, db:
                db.execute("UPDATE history SET amount=100")
                db.execute("INSERT INTO history VALUES('unexpected',100)")
            with self.assertRaisesRegex(ValueError, "preservation"):
                verify_preview(baseline, preview, [])


if __name__ == "__main__":
    unittest.main()
