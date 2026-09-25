---
description: The Zazr Agent Skill teaches your coding assistant to write idiomatic Zazr code. What it contains and how to install it.
---

# Zazr in your AI assistant

A coding assistant that has never seen Zazr writes Vavr, or plain Java. It reaches for `Option.of`, `Match` or a loop
of `append`, and the code does not compile or does not read like Zazr.

The Zazr Agent Skill fixes that. It is a folder of Markdown files, in the open
[Agent Skills](https://agentskills.io/) format, that the assistant reads when it works on code that uses Zazr.

## What it teaches

`SKILL.md` is short: what Zazr is, when the skill applies, and ten rules. The assistant loads it only when the task
involves Zazr.

The details are in reference files, read on demand:

- the control types: which one to pick, the members they share, conversions and sharp edges;
- the collections: which one to choose, what each operation costs, builders, `NonEmptyVector` and Java interop;
- `zip` and `zipWith` at arity 2 to 8;
- the Vavr names and types that Zazr removed or renamed, as a "not this, this" table;
- how to write functional Java 25 in general, with Zazr as the toolkit.

Every Java example of the skill is compiled and run by Zazr's build, like the examples of this site.

## Install it

Copy the [`skills/zazr` folder](https://github.com/guizmaii-opensource/zazr/tree/main/skills/zazr) into your
assistant's skills directory. In Claude Code, that is `.claude/skills/zazr/` in a project, or `~/.claude/skills/zazr/`
for every project of a user.

```bash
git clone --depth 1 https://github.com/guizmaii-opensource/zazr.git /tmp/zazr
mkdir -p .claude/skills
cp -R /tmp/zazr/skills/zazr .claude/skills/
```

Other assistants that support Agent Skills read the same folder from their own skills directory; their documentation
says where it is.

For an assistant without skills, paste `SKILL.md` into the project's instructions, and the reference files it needs
next to it.

## Keep it current

The skill describes the API of `main`, which changes before 1.0. Copy the folder again when you move to a newer
snapshot of Zazr.
