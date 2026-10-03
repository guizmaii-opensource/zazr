# Contributing to Zazr

Thanks for your interest. Zazr is small and opinionated, so a short conversation before the code saves everyone
time.

## Before you start

Open an issue or a discussion before starting anything non-trivial, so we can agree on the approach first. Bug
reports with a failing snippet are always welcome as they are.

Using AI tools to write code is fine. Make sure you understand every line you submit and can explain it: a pull
request that is easier to rewrite than to review is unlikely to be merged.

Every change follows the process in [PROCESS.md](PROCESS.md): an issue first, a pull request that fills in the
template, and an independent review on the maintainer's side before merging. If you work with agents, brief them
with that file.

The reasons behind the API are on the [Design](https://zazr.dev/principles/) page, and every decision is recorded,
with its alternatives, in the [decision log](docs/design.md). If a change goes against a decision there, say so in
the issue.

## Build

You need JDK 25 or later. Everything goes through the Makefile; `make help` lists the targets.

```bash
make verify                                   # what CI runs: tests, formatting, nullness and the checks below
make test-one TEST=VectorTest MODULE=zazr-core
make test-one TEST='DocsExamplesTest$$ValidationPage'  # a nested class: Make needs the $ doubled
make fmt                                      # format the sources (Palantir Java Format)
make site-serve                               # preview the website at http://127.0.0.1:8000/
make coverage                                 # test coverage reports, in <module>/target/site
```

`make verify` must pass before you open a pull request.

`zazr-core` must keep at least 95 % of its lines and 95 % of its branches covered, counting the tests of `zazr-core`
and `zazr-test` together, and `zazr-jackson` the same, counting its own tests. `make coverage` fails below either
figure, and so does the CI `coverage` job. The threshold
is on the module as a whole, not per file: JaCoCo never marks a line covered when the method it calls throws, such
as `return sneakyThrow(t);`, so a small class with such a line can stay below 95 % however well it is tested.
On a pull request, the `coverage` job posts the summary of `make coverage-summary` as a comment and edits that
same comment on every push.

Sources under `src-gen` are generated from `generator/Generator.scala` on every build. Change the generator, never
the generated files.

## Rules of the code

- **Names say what an operation does.** Use ZIO's vocabulary: `zip`, `zipWith`, `collectAll`, `forEach`,
  `mapBoth`, `tap`, `catchAll`, `flip`. Category-theory names (Monad, Functor, Applicative, `ap`, `traverse`, ...)
  are not used anywhere, code or comments; `make vocabulary` checks it.
- **No `null` inside.** `Some`, `Right`, `Success`, `Valid` and every collection reject it. A function that returns
  `null` where a value is expected is rejected with a message naming the method.
- **Modern Java.** Sealed interfaces, records, pattern-matching `switch`, the JDK's functional interfaces. No
  preview features, and no runtime dependencies.
- **Costs are documented.** Every method of a collection whose cost depends on its size states it in a
  `Complexity:` line of its javadoc. `make complexity` checks it, and the website's complexity page is generated
  from these lines.
- **Internal types stay internal.** They live in `.internal` packages, which are not exported and never appear in a
  public signature.
- **Comments describe the code as it is.** No ticket numbers and no history of how the code got there: that belongs
  in the commit message and the decision log.
- **Formatted by the Palantir Java Format.** 120 columns and no configuration: run `make fmt` before committing; the
  build fails on an unformatted file. Two limits of the formatter: keep the lines of a Markdown javadoc comment
  (`///`) within 120 columns minus their indentation, since the formatter breaks a longer one, and write `var _`
  rather than a bare `_` inside a record pattern (`case Some(Card(var _))`), which it cannot parse yet.
- **Locals are never reassigned.** A local variable or a parameter keeps the value it is declared with, and `final`
  is not written on it: `make reassignment` fails on a reassignment and on a redundant `final`. The variables of a
  `for` header are exempt; the state of a loop in the collection internals that must change is declared with
  `@SuppressWarnings("Var")`.
- **Measure before optimising.** Correctness comes first. Make a change for speed only in a performance ticket,
  where the measurement is agreed first, following [Rob Pike's rules](https://users.ece.utexas.edu/~adnan/pike.html).
- No license header in source files; the attribution to Vavr is in [NOTICE](NOTICE).

## Tests

- Every public method is tested, including its edge cases: empty and one-element inputs, `null` arguments, and
  size boundaries (for `Vector`, 31, 32, 33 and 1023, 1024, 1025 elements).
- Lazy operations are tested for what they force, not only for their result.
- Test names describe the behaviour: `shouldFooWhenBar`.
- Every Java snippet on the website also lives in a documentation test; `make docs-examples` checks that they match.

## Documentation

The website is written for a Java developer deciding whether and how to use Zazr: short paragraphs, one idea each,
a small example where it helps, and no internal names or ticket numbers. The method reference is the javadoc.

## Pull requests and commits

- Branch from `main` and open a pull request against it; `main` only changes through pull requests.
- Keep a pull request to one change. A large change is split into several pull requests, each building on the
  previous one (from a fork, open the next once the previous one is merged).
- Write commit messages that explain what changed and why, enough to write the release notes from them.

## Versions

Before 1.0, there is no compatibility promise: any 0.x release may change the API, and its release notes in
[GitHub Releases](https://github.com/guizmaii-opensource/zazr/releases) list the changes. From 1.0, Zazr follows
[Semantic Versioning](https://semver.org).

## Snapshots

`main` is always on the next `-SNAPSHOT` version. When snapshot publishing is enabled, the CI deploys every commit of
`main` to the Maven Central snapshot repository. To test a change against the latest code, build `main` locally
with `make install` and depend on its `-SNAPSHOT` version.

## Releases

Only the maintainer cuts a release. A release is automatic once the GitHub release is published:

1. Create a GitHub release whose tag is the version prefixed with `v` (for example `v0.1.0`), on a `main` commit whose
   CI is green, with the release notes in its description. There is no changelog file: the notes live in the release.
2. Publishing it starts the `release` workflow. It sets the version from the tag, builds and signs the artifacts,
   uploads them to the Maven Central Portal, and waits until Central has validated and published them. If Central
   rejects them, or does not publish them in time, the workflow fails; it can be run again by hand from the Actions
   tab, with the tag as its `release-tag` input.
3. When the workflow succeeds, check that the new version of each artifact is on
   [Maven Central](https://repo1.maven.org/maven2/dev/zazr/).
4. Bump `main` to the next snapshot version
   (`./mvnw versions:set -DnewVersion=<next version>-SNAPSHOT -DgenerateBackupPoms=false`) in a pull request.
