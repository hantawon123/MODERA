#!/usr/bin/env python3
"""Repair and validate the captured registration JSON against actual image files."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


def repair_json_strings(raw: str) -> str:
    result: list[str] = []
    in_string = False
    escaped = False
    for char in raw:
        if not in_string:
            result.append(char)
            if char == '"':
                in_string = True
            continue
        if escaped:
            if char in "\r\n":
                result.append("n")
            else:
                result.append(char)
            escaped = False
            continue
        if char == "\\":
            result.append(char)
            escaped = True
        elif char == '"':
            result.append(char)
            in_string = False
        elif char == "\r":
            continue
        elif char == "\n":
            result.append("\\n")
        elif ord(char) < 0x20:
            result.append(f"\\u{ord(char):04x}")
        else:
            result.append(char)
    if in_string:
        raise ValueError("unterminated JSON string")
    return "".join(result)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--requests", type=Path, required=True)
    parser.add_argument("--images", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    payload = json.loads(repair_json_strings(args.requests.read_text(encoding="utf-8-sig")))
    records = payload.get("images")
    if not isinstance(records, list) or not records:
        raise ValueError("root.images must be a non-empty array")

    actual = {path.name: path for path in args.images.iterdir() if path.is_file()}
    names = [item.get("fileName") for item in records]
    duplicate_names = sorted({name for name in names if names.count(name) > 1})
    missing = sorted(set(names) - set(actual))
    extra = sorted(set(actual) - set(names))
    errors: list[str] = []
    if duplicate_names:
        errors.append(f"duplicate request fileName: {duplicate_names}")
    if missing:
        errors.append(f"missing image files: {missing}")
    if extra:
        errors.append(f"images without requests: {extra}")

    for item in records:
        name = item.get("fileName")
        path = actual.get(name)
        if path is None:
            continue
        expected_size = item.get("fileSize")
        expected_hash = str(item.get("contentHash", "")).lower()
        real_size = path.stat().st_size
        real_hash = sha256(path)
        if expected_size != real_size:
            errors.append(f"{name}: size request={expected_size}, actual={real_size}")
        if expected_hash != real_hash:
            errors.append(f"{name}: sha256 request={expected_hash}, actual={real_hash}")

    if errors:
        raise ValueError("test-data validation failed:\n- " + "\n- ".join(errors))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps({"images": records}, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"validated {len(records)} request/image pairs -> {args.output}")


if __name__ == "__main__":
    main()
