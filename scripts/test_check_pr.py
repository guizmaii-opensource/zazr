"""Tests of check-pr.py. Run with `python3 scripts/test_check_pr.py`."""

import importlib.util
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("check_pr", HERE / "check-pr.py")
check_pr = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check_pr)
TEMPLATE = check_pr.DEFAULT_TEMPLATE

SECTIONS = check_pr.template_sections(TEMPLATE)


def body(closing="Closes #1", **overrides):
    text = []
    for name in SECTIONS:
        content = overrides.get(name, "Filled in, with a reason.")
        if name == "Ticket":
            content = closing + ("\n" + overrides["Ticket"] if "Ticket" in overrides else "")
        text.append(f"## {name}\n\n{content}\n")
    return "\n".join(text)


def problems(text, commits=()):
    return check_pr.check(text, list(commits), TEMPLATE)


class CheckPrTest(unittest.TestCase):
    def test_complete_body_passes(self):
        self.assertEqual(problems(body()), [])

    def test_template_sections(self):
        self.assertEqual(SECTIONS[0], "Ticket")
        self.assertIn("Mutation check", SECTIONS)

    def test_empty_template_fails_on_every_section(self):
        found = problems(TEMPLATE.read_text(encoding="utf-8"))
        self.assertIn("no `Closes #N` line: every pull request starts from an issue", found)
        self.assertEqual(sum("is empty" in p for p in found), len(SECTIONS) - 1)

    def test_closing_variants(self):
        for closing in ["Closes #12", "closes: #12", "Fixes #3", "fixed #3", "Resolves #9", "- Closes #12", "Close #1"]:
            self.assertEqual(problems(body(closing)), [], closing)
        for closing in ["Closes #", "See #12", "<!-- Closes #12 -->", "```\nCloses #12\n```"]:
            self.assertIn("no `Closes #N` line: every pull request starts from an issue", problems(body(closing)), closing)

    def test_missing_and_duplicate_sections(self):
        text = body().replace("## Docs", "## Documentation")
        self.assertIn("section `## Docs` is missing", problems(text))
        self.assertIn("section `## Tests` appears more than once", problems(body() + "\n## Tests\n\nagain\n"))

    def test_heading_forms(self):
        self.assertEqual(problems(body().replace("## Docs", "  ## Docs ##")), [])
        self.assertIn("section `## Docs` is missing", problems(body().replace("## Docs", "### Docs")))
        fenced = body().replace("## Docs\n", "```\n## Docs\n```\n")
        self.assertIn("section `## Docs` is missing", problems(fenced))

    def test_placeholders(self):
        for answer in ["none", "None.", "_None_", "__None__", "*none*", "N/A", "n/a", "TBD", "-", "—", ".", "nothing"]:
            self.assertIn("section `## Tests` says none without saying why", problems(body(Tests=answer)), answer)
        self.assertEqual(problems(body(Tests="None: documentation only.")), [])
        self.assertEqual(problems(body(**{"Found but not fixed": "None."})), [])
        self.assertEqual(problems(body(**{"Decisions beyond the ticket": "_none_"})), [])

    def test_comments(self):
        self.assertIn("section `## Tests` is empty", problems(body(Tests="<!-- to do -->")))
        unclosed = body(Tests="<!-- to do")
        self.assertTrue(any("is empty" in p or "missing" in p for p in problems(unclosed)))

    def test_crlf(self):
        self.assertEqual(problems(body().replace("\n", "\r\n")), [])

    def test_attribution(self):
        flagged = [
            "Co-Authored-By: Someone <noreply@anthropic.com>",
            "co-authored-by: A Person <a@b.c>",
            "Claude-Session: https://example.com/x",
            "🤖 Generated with [Claude Code](https://claude.com/claude-code)",
        ]
        for line in flagged:
            self.assertTrue(any("attribution line in the body" in p for p in problems(body() + "\n" + line)), line)
            found = problems(body(), [f"Subject\n\n{line}\n"])
            self.assertTrue(any("attribution line in commit `Subject`" in p for p in found), line)
        self.assertEqual(problems(body(Tests="No `Co-Authored-By` line is allowed.")), [])


if __name__ == "__main__":
    unittest.main()
