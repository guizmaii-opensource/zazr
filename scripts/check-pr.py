#!/usr/bin/env python3
"""Checks a pull request against PROCESS.md section 7.

The body is read from the file given with --body (or standard input), the commit messages from the file given with
--commits (the output of `git log --format=%B%x00 base..head`, messages separated by NUL). Exits with 1 and one line
per problem when the pull request does not follow the template.

Checks:
- a `Closes #N` (or `Fixes #N`, `Resolves #N`) line;
- every section of .github/pull_request_template.md present as a `## ` heading, with content once the template's
  HTML comments are removed; a bare "none" is accepted only where nothing is a normal answer (decisions beyond the
  ticket, found but not fixed), elsewhere a "none" says why;
- no attribution lines (`Co-Authored-By`, `Generated with`, session links) in the body or the commits.
"""

import argparse
import re
import sys
from pathlib import Path

TEMPLATE = Path(__file__).resolve().parent.parent / ".github" / "pull_request_template.md"

CLOSING = re.compile(r"^\s*(closes|fixes|resolves)\s+#\d+\b", re.IGNORECASE | re.MULTILINE)
COMMENT = re.compile(r"<!--.*?-->", re.DOTALL)
BARE_NONE = re.compile(r"^\W*(none|n/?a|nothing|-+)\W*$", re.IGNORECASE)
# Sections where "none" alone is a complete answer; elsewhere a "none" must say why.
NONE_ALLOWED = {"decisions beyond the ticket", "found but not fixed"}
ATTRIBUTION = re.compile(
    r"co-authored-by:|generated with|claude-session:|claude\.ai/code|noreply@anthropic\.com", re.IGNORECASE
)


def template_sections() -> list[str]:
    return re.findall(r"^## (.+?)\s*$", TEMPLATE.read_text(encoding="utf-8"), re.MULTILINE)


def sections(body: str) -> dict[str, str]:
    found: dict[str, str] = {}
    parts = re.split(r"^## (.+?)\s*$", body, flags=re.MULTILINE)
    for i in range(1, len(parts), 2):
        found[parts[i].strip().lower()] = parts[i + 1]
    return found


def check(body: str, commits: list[str]) -> list[str]:
    problems: list[str] = []
    body = body.replace("\r\n", "\n")
    if not CLOSING.search(COMMENT.sub("", body)):
        problems.append("no `Closes #N` line: every pull request starts from an issue")
    found = sections(body)
    for name in template_sections():
        content = found.get(name.lower())
        if content is None:
            problems.append(f"section `## {name}` is missing")
            continue
        text = COMMENT.sub("", content).strip()
        if not text:
            problems.append(f"section `## {name}` is empty")
        elif BARE_NONE.match(text) and name.lower() not in NONE_ALLOWED:
            problems.append(f"section `## {name}` says none without saying why")
    for line in body.splitlines():
        if ATTRIBUTION.search(line):
            problems.append(f"attribution line in the body: {line.strip()}")
    for message in commits:
        for line in message.splitlines():
            if ATTRIBUTION.search(line):
                subject = message.strip().splitlines()[0] if message.strip() else "?"
                problems.append(f"attribution line in commit `{subject}`: {line.strip()}")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--body", help="file holding the pull request body (default: standard input)")
    parser.add_argument("--commits", help="file holding the commit messages, separated by NUL")
    args = parser.parse_args()
    body = Path(args.body).read_text(encoding="utf-8") if args.body else sys.stdin.read()
    commits = Path(args.commits).read_text(encoding="utf-8").split("\0") if args.commits else []
    problems = check(body, commits)
    for problem in problems:
        print(f"PROCESS.md section 7: {problem}")
    if problems:
        print("Fill in the pull request description from .github/pull_request_template.md (see PROCESS.md).")
        return 1
    print(f"pull request follows the template ({len(template_sections())} sections, {len(commits)} commits checked)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
