#!/usr/bin/env python3
"""Preflight or publish a resolved KissMod release plan."""

from __future__ import annotations

import argparse
import json
import pathlib
import sys

from release_support.core import PublicationError
from release_support.transaction import preflight, publish


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("preflight", "publish"))
    parser.add_argument("--plan", type=pathlib.Path, required=True)
    parser.add_argument("--artifact-dir", type=pathlib.Path)
    parser.add_argument("--repository")
    parser.add_argument(
        "--recovery",
        type=pathlib.Path,
        default=pathlib.Path("release-recovery/release-recovery.json"),
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        plan = json.loads(args.plan.read_text(encoding="utf-8"))
        if args.command == "preflight":
            preflight(plan, args.repository)
        else:
            if not args.artifact_dir or not args.repository:
                raise PublicationError(
                    "publish requires --artifact-dir and --repository"
                )
            publish(plan, args.artifact_dir, args.repository, args.recovery)
        return 0
    except (OSError, ValueError, PublicationError, json.JSONDecodeError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
