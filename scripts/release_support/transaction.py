from __future__ import annotations

import os
import pathlib
import shutil
from typing import Any

from .core import HttpClient, PublicationError, RecoveryJournal, sha256
from .curseforge import CurseForgePlatform
from .github import GitHubPlatform
from .modrinth import ModrinthPlatform


REQUIRED_CREDENTIALS = (
    "GITHUB_TOKEN",
    "MODRINTH_TOKEN",
    "CURSEFORGE_TOKEN",
    "MODRINTH_PROJECT_ID",
    "CURSEFORGE_PROJECT_ID",
)


def validate_artifacts(
    plan: dict[str, Any], artifact_dir: pathlib.Path
) -> list[pathlib.Path]:
    artifacts = [artifact_dir / row["artifact_name"] for row in plan["rows"]]
    missing = [str(path) for path in artifacts if not path.is_file()]
    if missing:
        raise PublicationError("Missing release artifacts: " + ", ".join(missing))
    return artifacts


def write_checksums(
    artifacts: list[pathlib.Path], output: pathlib.Path
) -> pathlib.Path:
    output.write_text(
        "".join(f"{sha256(path)}  {path.name}\n" for path in artifacts),
        encoding="utf-8",
    )
    return output


def require_environment(names: tuple[str, ...]) -> dict[str, str]:
    values = {name: os.environ.get(name, "") for name in names}
    missing = [name for name, value in values.items() if not value]
    if missing:
        raise PublicationError(
            "Missing required environment variables: " + ", ".join(missing)
        )
    return values


def create_platforms(
    repository: str, credentials: dict[str, str], backup_dir: pathlib.Path
) -> tuple[GitHubPlatform, ModrinthPlatform, CurseForgePlatform]:
    client = HttpClient()
    return (
        GitHubPlatform(client, repository, credentials["GITHUB_TOKEN"], backup_dir),
        ModrinthPlatform(
            client,
            credentials["MODRINTH_PROJECT_ID"],
            credentials["MODRINTH_TOKEN"],
            backup_dir,
            repository,
        ),
        CurseForgePlatform(
            client,
            credentials["CURSEFORGE_PROJECT_ID"],
            credentials["CURSEFORGE_TOKEN"],
        ),
    )


def preflight_platforms(
    plan: dict[str, Any],
    github: GitHubPlatform,
    modrinth: ModrinthPlatform,
    curseforge: CurseForgePlatform,
) -> None:
    github.preflight(plan)
    modrinth.preflight(plan)
    curseforge.preflight(plan)


def run_transaction(
    plan: dict[str, Any],
    artifacts: list[pathlib.Path],
    github: GitHubPlatform,
    modrinth: ModrinthPlatform,
    curseforge: CurseForgePlatform,
    journal: RecoveryJournal,
) -> None:
    github_assets = [*artifacts, artifacts[0].parent / "SHA256SUMS.txt"]
    github_state: dict[str, Any] | None = None
    modrinth_states: list[dict[str, Any]] = []
    try:
        github_state = github.publish(plan, github_assets, journal)
        for row, artifact in zip(plan["rows"], artifacts, strict=True):
            modrinth_states.append(modrinth.publish_row(plan, row, artifact, journal))
        for row, artifact in zip(plan["rows"], artifacts, strict=True):
            try:
                curseforge.upload_row(plan, row, artifact, journal)
            except Exception:
                journal.manual_actions.append(
                    "Check CurseForge manually for "
                    f"{row['external_version']}; the upload response was unsuccessful or "
                    "ambiguous and author tokens cannot query file hashes."
                )
                raise
    except Exception as exc:
        _compensate(
            github, modrinth, github_state, modrinth_states, github_assets, journal
        )
        raise PublicationError(
            f"Release failed and compensating rollback was attempted: {exc}"
        ) from exc


def _compensate(
    github: GitHubPlatform,
    modrinth: ModrinthPlatform,
    github_state: dict[str, Any] | None,
    modrinth_states: list[dict[str, Any]],
    github_assets: list[pathlib.Path],
    journal: RecoveryJournal,
) -> None:
    if any(operation["platform"] == "curseforge" for operation in journal.operations):
        journal.manual_actions.append(
            "Delete every CurseForge file ID recorded in operations; "
            "CurseForge exposes no file-delete API."
        )
    for state in reversed(modrinth_states):
        try:
            modrinth.rollback_row(state)
        except Exception as exc:
            journal.rollback_errors.append(f"Modrinth rollback failed: {exc}")
    if github_state:
        try:
            github.rollback(github_state, github_assets)
        except Exception as exc:
            journal.rollback_errors.append(f"GitHub rollback failed: {exc}")


def publish(
    plan: dict[str, Any],
    artifact_dir: pathlib.Path,
    repository: str,
    recovery_path: pathlib.Path,
) -> None:
    credentials = require_environment(REQUIRED_CREDENTIALS)
    artifacts = validate_artifacts(plan, artifact_dir)
    write_checksums(artifacts, artifact_dir / "SHA256SUMS.txt")
    journal = RecoveryJournal(plan["tag"])
    backup_dir = recovery_path.parent / "backups"
    github, modrinth, curseforge = create_platforms(repository, credentials, backup_dir)
    succeeded = False
    try:
        # Repeat every read-only check immediately before the first mutation.
        # This closes the race between the prepare job and the publication job.
        preflight_platforms(plan, github, modrinth, curseforge)
        run_transaction(plan, artifacts, github, modrinth, curseforge, journal)
        succeeded = True
    finally:
        journal.write(recovery_path)
        if succeeded:
            shutil.rmtree(backup_dir, ignore_errors=True)


def preflight(plan: dict[str, Any], repository: str | None = None) -> None:
    credentials = require_environment(REQUIRED_CREDENTIALS)
    repository = repository or os.environ.get("GITHUB_REPOSITORY", "")
    if not repository:
        raise PublicationError(
            "Missing required environment variable: GITHUB_REPOSITORY"
        )
    github, modrinth, curseforge = create_platforms(
        repository, credentials, pathlib.Path(".release-preflight")
    )
    preflight_platforms(plan, github, modrinth, curseforge)
