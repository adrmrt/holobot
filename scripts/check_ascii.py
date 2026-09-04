#!/usr/bin/env python3
"""Fail if any .java source file under src/ contains non-ASCII characters
outside of string/char literals (regular strings, text blocks, char literals).

Comments and identifiers must stay ASCII-only; literal data (e.g. Discord
embed text, emoji) is exempt since it's explicit, intentional content.
"""
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

SRC_DIR = Path(__file__).resolve().parent.parent / "src"


def find_violations(text: str) -> list[tuple[int, int, str]]:
    violations = []
    i = 0
    n = len(text)
    line = 1
    col = 1

    def advance(count: int = 1) -> None:
        nonlocal i, line, col
        for _ in range(count):
            if i < n and text[i] == "\n":
                line += 1
                col = 1
            else:
                col += 1
            i += 1

    while i < n:
        c = text[i]

        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                advance()
            continue

        if c == "/" and i + 1 < n and text[i + 1] == "*":
            advance(2)
            while i < n and not (text[i] == "*" and i + 1 < n and text[i + 1] == "/"):
                if ord(text[i]) > 0x7F:
                    violations.append((line, col, text[i]))
                advance()
            advance(2)
            continue

        if text[i:i + 3] == '"""':
            advance(3)
            while i < n and text[i:i + 3] != '"""':
                advance()
            advance(3)
            continue

        if c == '"':
            advance()
            while i < n and text[i] != '"':
                if text[i] == "\\" and i + 1 < n:
                    advance(2)
                else:
                    advance()
            advance()
            continue

        if c == "'":
            advance()
            while i < n and text[i] != "'":
                if text[i] == "\\" and i + 1 < n:
                    advance(2)
                else:
                    advance()
            advance()
            continue

        if ord(c) > 0x7F:
            violations.append((line, col, c))
        advance()

    return violations


def main() -> int:
    failed = False
    for path in sorted(SRC_DIR.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for line, col, char in find_violations(text):
            failed = True
            rel = path.relative_to(SRC_DIR.parent)
            print(f"{rel}:{line}:{col}: non-ASCII character {char!r} outside a string/char literal")

    if failed:
        print("\nNon-ASCII characters are only allowed inside string/char literals.")
        return 1

    print("OK: no non-ASCII characters found outside string/char literals.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
