# How work is done on Zazr

This is the procedure every change to Zazr follows, whoever does the work: the maintainer, a contributor, or agents
working for either. It says who does what, what a pull request must contain, how it is reviewed, and what evidence
must be left on GitHub so that anyone can check the procedure was followed.

`CLAUDE.md` holds the project's rules (language, naming, build, documentation). This file holds the process. Where
the two disagree, this file wins for the process and `CLAUDE.md` for the code.

## 1. The rule of evidence

**Only what is on GitHub counts.** A step that left no trace on the issue or the pull request did not happen. A claim
("the tests pass", "I checked for nulls") counts only with the evidence that lets someone else check it: a CI run, a
test name, a command and its output, a reproducer.

Nobody reviews their own work as the gate. The review that decides whether a pull request is ready is always done by
someone other than its author, starting from no context other than the repository, the ticket and the pull request.
For a contributor's pull request, that review is run on the maintainer's side (section 6).

## 2. Roles

| Role | Does | Never does |
|---|---|---|
| **Maintainer** | Decides direction, API shape and release scope. Answers design questions. Merges. Owns the accounts (Maven Central, DNS, secrets). | |
| **Coordinator** | Turns requests into tickets, briefs implementers, reviews every pull request, starts the independent review, triages findings, keeps the trackers current, tells the maintainer what is ready and in which order to merge. Decides small reversible things and logs them. | Merge. Commit on `main`. Write large features itself. |
| **Implementer** | One per ticket or per step. Works in its own git worktree, writes the code, tests and docs, opens the pull request, answers every review comment, merges `main` into its branch when needed. | Merge. Force-push. Rebase a pushed branch. Touch another branch. |
| **Independent reviewer** | One fresh reviewer per pull request. Checks correctness only, with its own probes, and posts one review with a verdict. | Style or wording opinions, benchmarks, commits, pushes. |

A contributor with their own agents plays the coordinator and implementer roles for their own work. The independent
review of their pull requests is not theirs to run (section 6).

## 3. The loop, per unit of work

1. **Ticket.** Every change starts from a GitHub issue: what and why, the decision taken, the scope, the tests
   expected, what is out of scope, and where it comes from (a request, a review finding, an audit). A decision taken
   in a conversation is written into the ticket ("Decided by the maintainer (date, link to the maintainer's
   comment): ..."). A decision with no such link does not count.
2. **Brief.** The coordinator starts one implementer per ticket with a self-contained brief (section 4).
3. **Pull request.** The implementer opens it with the body from the template (section 7), `Closes #N`, and a green
   CI, then reports: the link, the decisions taken beyond the ticket, anything the maintainer must decide.
4. **Coordinator review.** The coordinator checks the structure (author, no attribution lines, CI state, mergeable,
   files touched), reads the risky parts, checks the claims, and posts a comment review (`"event": "COMMENT"`: GitHub
   refuses approve and request-changes on a pull request from the same account) whose first line is the verdict. It
   accepts or pushes back on each decision beyond the ticket, in writing.
5. **Independent review.** A fresh reviewer, briefed only with the repository, the pull request and the questions to
   answer (section 5), posts one review.
6. **Triage.** Findings go back to the implementer who wrote the code. A correctness defect is fixed, then gets a
   second independent review scoped to the fix. A documentation or test-coverage finding is fixed and checked by the
   coordinator, with no further review. Every finding gets a written answer on the pull request: fixed (with the
   commit), kept (with the reason), or filed as a new issue (with its number).
7. **Ready.** When the coordinator's check is clean and the last independent review found no correctness defect (or
   its defects are fixed and checked), the coordinator checks the pull request's trail (section 7) and comments
   "Ready to merge" on the pull request, with a link to each item of the trail and the head commit it covers, then
   tells the maintainer. A push after that comment voids it. The maintainer merges only a pull request with that
   comment, posted from the maintainer's account, with `gh pr merge <N> --squash --match-head-commit <sha> --repo
   guizmaii-opensource/zazr`.
8. **After the merge.** Check `main`'s CI. Find the open pull requests that now conflict and have their implementers
   merge `main` in (a merge commit). Update the tracker.

**When reviews stop.** A second independent review happens only when the first found a real correctness defect. When
successive reviews find narrower and narrower edge cases, the coordinator checks the last fix itself and says so on
the pull request. Every pull request gets at least one independent review, including small documentation and
configuration changes.

## 4. Rules for implementers

A brief is self-contained: the ticket number, the base branch, the branch name, the base of the pull request, and a
pointer to this section. The implementer follows all of it.

- Read `CLAUDE.md` first and follow it entirely.
- Work only in your own git worktree, never in a shared checkout.
- **Never use `git stash`.** The stash is shared by every worktree of a repository: two people or agents stashing at
  once swap each other's changes. To compare with old code, use a second detached worktree or
  `git show origin/main:<path>`.
- Keep temporary files (pull request bodies, probes, logs) inside your own worktree or a folder named after your
  branch, never at a shared path.
- Pass your pull request number explicitly to every `gh pr edit` and `gh pr comment`, and `--repo
  guizmaii-opensource/zazr` to every `gh` command.
- Never `pkill` or `killall` by name: other people's builds run on the same machine. Kill only a process you
  started.
- **Stacked pull requests.** A step that depends on an unmerged pull request branches from it and opens its pull
  request with `--base <that branch>`. When the base moves, merge it in with a merge commit. When the base is
  squash-merged, the stacked pull request retargets to `main` and usually conflicts: merge `main` in, and where a
  conflict is "the base's content" against "the base's content plus mine", take your side. Never rebase, never
  force-push. You never merge a pull request.
- **Correctness first, no benchmarking.** No JMH, no timing or allocation probes. If a rewrite exists only for speed
  and you cannot tell it is faster without measuring, keep the existing code and leave a note. Performance work is
  its own tickets.
- **Tests:** every new public method, every branch and fast path, the boundaries (0, 1, 2, 31, 32, 33, 1023, 1024,
  1025 for the tries), nulls, one-shot iterables, older versions of a persistent collection staying valid, reuse
  after close, each concrete representation an input can have (`Object[]` versus primitive leaves).
- **Mutation check:** break the new code on purpose (swap a condition, drop a branch, return the other side) and
  confirm a test fails. Record the change, as a diff, and the test that failed in the pull request.
- **No attribution lines.** No `Co-Authored-By`, "Generated with" or session lines in commits, pull requests,
  comments, code or docs. Commits are written from the author's perspective, under the author's own identity.
- `make verify` passes before you push. CI is green before you report. Answer every review comment in its thread,
  including bot comments, after checking them against the code; a wrong comment gets a short reply saying why.
- Remove your worktree at the end.

## 5. Rules for independent reviewers

- Start fresh: no context from whoever wrote or coordinated the change. Work in your own detached worktree, plus one
  of `origin/main` for comparisons. The same safety rules as implementers apply (no stash, no `pkill`, one build at a
  time).
- **Scope: correctness only.** Defects, broken contracts and invariants, unsafe sharing or mutation, edge cases, and
  the tests that would have caught them. Nothing on style, wording or performance, and no benchmarks.
- Answer the concrete questions in the brief, and anything else you find. Typical probes:
  - differential tests against `main` or a reference (the JDK's collections, a model, Scala's behaviour);
  - breaking the code on purpose, to confirm the tests catch it;
  - boundary sizes, nulls, one-shot inputs;
  - races, laziness counts, stack depth, persistence;
  - compile probes for API ambiguity;
  - the release build for packaging changes.
- **Deliverable:** one review on the pull request (a comment review). The body starts with exactly
  `Verdict: no correctness defect found` or `Verdict: correctness defect(s) found`, then says the commit reviewed and
  what was checked and how, then each finding with a reproducer (the input, the expected result, the actual
  result). Put the probe code in a collapsed `<details>` block so anyone can rerun it. Inline comments where a finding
  has a line.
- On a contributor's pull request, reapply at least one recorded mutation and confirm that the named test fails.
  Say so in the review.
- Notes outside the pull request's scope (an adjacent bug, an old defect) are reported as notes; the coordinator
  files them as issues.

## 6. Contributors and their agents

Contributions made with agents are welcome under the same process, with one difference: **the gate is on the
maintainer's side.**

- Start from an issue, and agree on the approach there before writing code (see `CONTRIBUTING.md`).
- Follow sections 3 to 5 and 7 for your own work: brief your implementers with section 4, and review your own pull
  requests as a coordinator would. Your own independent reviews are welcome and will be read, but they are advisory. Do
  not post "Ready to merge": that comment belongs to the maintainer's coordinator.
- Once your pull request is open, the maintainer's side runs its own coordinator review and its own independent
  review, from scratch. Findings are answered on the pull request as in section 3, step 6.
- Commit under your own identity. Everything in section 4 applies, including no attribution lines.
- Open the pull request from your fork with `gh pr create --repo guizmaii-opensource/zazr --base main --head
  <user>:<branch>`. From a fork you cannot stack pull requests, since a base must be a branch of this repository:
  open the next step once the previous one is merged, or ask the maintainer to push a base branch.
- On your first pull request, CI starts only once the maintainer approves the workflow run. Report the pull request
  once CI is green.
- The maintainer's side keeps the tracker and the milestones up to date for your pull request (section 8).
- A pull request whose body does not follow the template, or whose claims cannot be checked from what is on GitHub,
  is sent back before any review.

## 7. What a pull request must contain

The pull request template (`.github/pull_request_template.md`) has these sections. Each is filled in, or says
"none" with a reason; "none" alone is enough for the decisions beyond the ticket and for what was found but not
fixed. The `pr-process` CI check (`scripts/check-pr.py`, tested by `scripts/test_check_pr.py`) fails a pull
request when:

- the description has no closing line (`Closes #N`, `Fixes #N` or `Resolves #N`);
- a section is missing, repeated or empty;
- a section holds only a placeholder ("none", "n/a", "tbd", a dash) outside the two sections above;
- the description or a commit carries an attribution trailer (`Co-Authored-By:`), or a tool footer or address.

Comments and code blocks don't count. The check runs again when the description is edited, and it uses the checker and
template of the base branch, so a pull request cannot loosen its own check. Dependabot's pull requests are skipped.
The check proves the sections are there, not that they are true: the reviews check the content.

- **Ticket:** `Closes #N`, and the release tracker it belongs to.
- **What changed and why**, in a few lines, with the behaviour before and after.
- **Decisions beyond the ticket:** each one, with its alternatives and why this one. The coordinator accepts or
  rejects each in its review.
- **Tests:** the test classes and what they cover, by the categories of section 4.
- **Mutation check:** the change made on purpose, as a diff in a collapsed `<details>` block, and the name of the
  test that failed, so that anyone can reapply it.
- **Checks run:** the commands (`make verify`, and `make site` when docs changed), on which commit, and the result.
  This section says what was run before pushing; it is not evidence for the gate. Only CI on the head commit is.
- **Docs:** the website pages, the Agent Skill references and the decision log entries updated, or why none were
  needed.
- **Found but not fixed:** anything noticed outside the scope, with the issue filed for it.

The trail a finished pull request leaves, in order:

1. the ticket;
2. the pull request with the sections above;
3. green CI on the last commit;
4. the coordinator's review, verdict first;
5. the independent review, verdict first, with its probes (a verdict with no rerunnable probe does not count);
6. a written answer to every finding;
7. the "Ready to merge" comment.

Checking the trail is the coordinator's job, on the maintainer's side: before its "Ready to merge" comment, it confirms
every item is there and links each one in that comment (the ticket, the CI run, each review, each answer to a finding).
A contributor's pull request is checked by the maintainer's coordinator, never by the contributor's own: on it, items 4,
5 and 7 count only when posted from the maintainer's account (@guizmaii). A verdict or a "Ready to merge" from any other
account is advisory, whatever it says. Anything missing sends the pull request back, and the comment says what is
missing.

## 8. Tickets, trackers and decisions

- **One tracker issue per release**, with a checklist in dependency order; each item links its ticket and, once
  done, its pull request. It is updated after every batch of merges or new tickets. A release has a milestone;
  non-blocking work moves to the next one, with the reason.
- Review findings and audit results become issues with their evidence (file and line, reproducer).
- **The maintainer decides** direction, API shape, taste, release scope, and anything outward-facing (publishing,
  accounts, merging). Questions to the maintainer come with a recommendation and the trade-off.
- **The coordinator decides** small reversible things (one of two equivalent implementations, a message's wording, a
  test strategy, an implementer's reasonable deviation) and writes each decision on the pull request or in the
  decision log (`docs/design.md`), so the maintainer can review them later.
- Standing rules: copy what Scala 2.13+ does unless the decision log says otherwise; correctness before performance;
  release notes live in GitHub Releases, with no changelog file; fixes are made in Zazr, not proposed upstream to
  Vavr.

## 9. Parallel work and merging

- Independent tickets run in parallel, planned by the files they touch: two pull requests editing the same files will
  conflict. A change that touches every file (a formatter, a package rename) runs alone.
- The coordinator gives the maintainer an explicit merge order. Pull requests that touch many files (generated pages,
  notes in every file) are merged as soon as they are green; sweeping mechanical changes go last.
- Two pull requests that are green on their own can break `main` together. After a batch of merges, check `main`'s CI
  and fix a break with a small pull request at once.
- Stacked pull requests get no `ci` or `site` run while their base is not `main` (both trigger only on pull
  requests to `main`). Run `make verify` locally and say so. Trail item 3 is met only by the CI run after the pull
  request is retargeted to `main`.

## 10. Lessons behind these rules

Each rule above came from an incident:

- Two agents used `git stash` in worktrees of one repository and swapped each other's changes.
- Two agents wrote their pull request body to the same scratch file: one pull request got the other's text, with a
  `Closes #N` that would have closed the wrong issue.
- An agent killed test processes by name, and other agents' builds died.
- A squash merge left the stacked pull request above it in conflict with its own base's content.
- A squash merge broke `.git-blame-ignore-revs`, which listed branch commits: list the squash commit instead.
- A local check that silently did nothing ("Nothing to compile") let an error reach CI: plant an error once to see a
  check fail before trusting it.
- Timing-based tests flaked under load: count the work instead, or make the gap between old and new behaviour huge.
- A rename script renamed identifiers it should not have: review a script's rules and list every occurrence it kept.
- Agents spent time on benchmarks nobody asked for, so briefs forbid them.
- Too many agents at once overloaded the machine: cap how many run at once, one build at a time each.
