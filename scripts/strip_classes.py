#!/usr/bin/env python3
import re
import sys

CLASS_RE = re.compile(r'class="([^"]*)"')


def strip_classes(text: str) -> str:
    def repl(m):
        tokens = m.group(1).split()
        if not tokens:
            return m.group(0)
        kept = [tokens[0]]
        if "hidden" in tokens[1:]:
            kept.append("hidden")
        return f'class="{" ".join(kept)}"'
    return CLASS_RE.sub(repl, text)


def main():
    for path in sys.argv[1:]:
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        with open(path, "w", encoding="utf-8") as f:
            f.write(strip_classes(content))


if __name__ == "__main__":
    main()
