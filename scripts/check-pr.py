#!/usr/bin/env python3
"""Checks a pull request against PROCESS.md section 7.

The body is read from the file given with --body (or standard input), the commit messages from the file given with
--commits (the output of `git log --format=%B%x00 base..head`, messages separated by NUL), and the section names from
--template (default: .github/pull_request_template.md next to this script). Exits with 1 and one line per problem
when the pull request does not follow the template.

Checks:
- a closing line for an issue (`Closes #N`, `Fixes #N`, `Resolves #N`, with GitHub's variants), outside comments and
  code blocks;
- every section of the template present once as a `## ` heading outside code blocks, with content once HTML comments
  are removed; a placeholder answer (only punctuation, "none", "n/a", "tbd"...) is accepted only where nothing is a
  normal answer (decisions beyond the ticket, found but not fixed), elsewhere a "none" says why;
- no attribution lines (trailers and tool footers) in the body or the commits.
"""

import argparse
import re
import sys
from pathlib import Path

DEFAULT_TEMPLATE = Path(__file__).resolve().parent.parent / ".github" / "pull_request_template.md"

HEADING = re.compile(r"^ {0,3}## +(.+?)(?: +#+)? *$", re.MULTILINE)
CLOSING = re.compile(
    r"^ *(?:[-*] +)?(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?):? +#\d+\b", re.IGNORECASE | re.MULTILINE
)
# An unclosed comment hides the rest of the body, as it does when GitHub renders it.
COMMENT = re.compile(r"<!--.*?(?:-->|\Z)", re.DOTALL)
FENCE = re.compile(r"^ {0,3}(```|~~~).*?^ {0,3}\1[^\n]*$", re.DOTALL | re.MULTILINE)
PLACEHOLDER = re.compile(r"^[\W_]*(?:none|n/?a|nothing|tbd|todo)?[\W_]*$", re.IGNORECASE)
# Sections where "none" alone is a complete answer; elsewhere a "none" must say why.
NONE_ALLOWED = {"decisions beyond the ticket", "found but not fixed"}
# Trailer lines and tool footers, not a mention of them in prose (`Co-Authored-By` in backquotes passes).
ATTRIBUTION = re.compile(
    r"^\s*(co-authored-by|claude-session):\s*\S"
    r"|generated with \[?claude|claude\.ai/code|noreply@anthropic\.com",
    re.IGNORECASE,
)


def template_sections(template: Path) -> list[str]:
    return HEADING.findall(COMMENT.sub("", template.read_text(encoding="utf-8")))


def blank_code(text: str) -> str:
    """Comments and code blocks replaced by as many blank lines, so that nothing inside them counts."""
    text = COMMENT.sub(lambda m: "\n" * m.group(0).count("\n"), text)
    return FENCE.sub(lambda m: "\n" * m.group(0).count("\n"), text)


def sections(body: str) -> tuple[dict[str, str], list[str]]:
    found: dict[str, str] = {}
    duplicates: list[str] = []
    parts = HEADING.split(body)
    for i in range(1, len(parts), 2):
        name = parts[i].strip().lower()
        if name in found:
            duplicates.append(parts[i].strip())
        else:
            found[name] = parts[i + 1]
    return found, duplicates


def check(body: str, commits: list[str], template: Path) -> list[str]:
    problems: list[str] = []
    body = body.replace("\r\n", "\n")
    visible = blank_code(body)
    if not CLOSING.search(visible):
        problems.append("no `Closes #N` line: every pull request starts from an issue")
    found, duplicates = sections(visible)
    for name in duplicates:
        problems.append(f"section `## {name}` appears more than once")
    for name in template_sections(template):
        content = found.get(name.lower())
        if content is None:
            problems.append(f"section `## {name}` is missing")
            continue
        text = content.strip()
        if not text:
            problems.append(f"section `## {name}` is empty")
        elif PLACEHOLDER.match(text) and name.lower() not in NONE_ALLOWED:
            problems.append(f"section `## {name}` says none without saying why")
    for line in body.splitlines():
        if ATTRIBUTION.search(line):
            problems.append(f"attribution line in the body: {line.strip()}")
    for message in commits:
        for line in message.splitlines():
            if ATTRIBUTION.search(line):
                subject = message.strip().splitlines()[0]
                problems.append(f"attribution line in commit `{subject}`: {line.strip()}")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--body", help="file holding the pull request body (default: standard input)")
    parser.add_argument("--commits", help="file holding the commit messages, separated by NUL")
    parser.add_argument("--template", type=Path, default=DEFAULT_TEMPLATE, help="the pull request template")
    args = parser.parse_args()
    body = Path(args.body).read_text(encoding="utf-8") if args.body else sys.stdin.read()
    commits = []
    if args.commits:
        commits = [m for m in Path(args.commits).read_text(encoding="utf-8").split("\0") if m.strip()]
    problems = check(body, commits, args.template)
    for problem in problems:
        print(f"PROCESS.md section 7: {problem}")
    if problems:
        print("Fill in the pull request description from .github/pull_request_template.md (see PROCESS.md).")
        return 1
    count = len(template_sections(args.template))
    print(f"pull request follows the template ({count} sections, {len(commits)} commits checked)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
