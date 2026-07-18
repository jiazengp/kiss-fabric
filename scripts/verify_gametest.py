"""Fail unless a Minecraft GameTest run executed and passed at least one test."""

from __future__ import annotations

import re
import sys
from pathlib import Path


RUNNING = re.compile(r"(\d+) tests are now running(?:!|\s)")
COMPLETE = re.compile(r"All (\d+) required tests passed")


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: verify_gametest.py <log>", file=sys.stderr)
        return 2

    text = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
    running = [int(match.group(1)) for match in RUNNING.finditer(text)]
    complete = [int(match.group(1)) for match in COMPLETE.finditer(text)]
    if not running or not complete:
        print("GameTest result markers were not found", file=sys.stderr)
        return 1
    if max(running) < 1 or max(complete) < 1:
        print("GameTest executed zero tests", file=sys.stderr)
        return 1
    if max(complete) < max(running):
        print("GameTest completion count is smaller than the execution count", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
