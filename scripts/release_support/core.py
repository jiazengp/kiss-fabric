from __future__ import annotations

import hashlib
import json
import pathlib
from dataclasses import dataclass, field
from typing import Any

import httpx


class PublicationError(RuntimeError):
    """A safe, user-facing publication failure."""


class HttpClient:
    """Shared HTTPX transport with explicit platform status validation."""

    def __init__(
        self, timeout: float = 90.0, client: httpx.Client | None = None
    ) -> None:
        self._client = client or httpx.Client(timeout=timeout, follow_redirects=True)

    def request(
        self,
        method: str,
        url: str,
        *,
        headers: dict[str, str] | None = None,
        body: bytes | None = None,
        form: dict[str, str] | None = None,
        files: list[tuple[str, tuple[str, bytes, str]]] | None = None,
        expected: tuple[int, ...] = (200,),
    ) -> tuple[int, dict[str, str], bytes]:
        try:
            response = self._client.request(
                method, url, headers=headers, content=body, data=form, files=files
            )
        except httpx.HTTPError as exc:
            raise PublicationError(f"{method} {url} failed: {exc}") from exc
        status = response.status_code
        data = response.content
        if status not in expected:
            detail = data.decode("utf-8", errors="replace")[:1000]
            raise PublicationError(f"{method} {url} returned HTTP {status}: {detail}")
        return status, dict(response.headers), data

    def json(
        self,
        method: str,
        url: str,
        *,
        headers: dict[str, str] | None = None,
        payload: Any | None = None,
        expected: tuple[int, ...] = (200,),
    ) -> tuple[int, Any]:
        request_headers = dict(headers or {})
        body = None
        if payload is not None:
            request_headers["Content-Type"] = "application/json"
            body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        status, _, data = self.request(
            method, url, headers=request_headers, body=body, expected=expected
        )
        if not data:
            return status, None
        try:
            return status, json.loads(data)
        except json.JSONDecodeError as exc:
            raise PublicationError(f"{method} {url} returned invalid JSON") from exc


def file_digest(path: pathlib.Path, algorithm: str) -> str:
    digest = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def sha256(path: pathlib.Path) -> str:
    return file_digest(path, "sha256")


@dataclass
class RecoveryJournal:
    tag: str
    operations: list[dict[str, Any]] = field(default_factory=list)
    rollback_errors: list[str] = field(default_factory=list)
    manual_actions: list[str] = field(default_factory=list)

    def record(self, platform: str, action: str, **details: Any) -> None:
        self.operations.append({"platform": platform, "action": action, **details})

    def write(self, path: pathlib.Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            json.dumps(
                {
                    "schema_version": 1,
                    "tag": self.tag,
                    "operations": self.operations,
                    "rollback_errors": self.rollback_errors,
                    "manual_actions": self.manual_actions,
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
