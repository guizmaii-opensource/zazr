---
description: What the next Zazr releases bring, with a link to each release's milestone on GitHub.
---

# Roadmap

Zazr ships small releases often, each with one theme. Each theme below links its milestone on GitHub, which lists
the work and shows where it stands.

!!! tip ""

    Wanna influence this roadmap? Open a ticket here: [GitHub issues](https://github.com/guizmaii-opensource/zazr/issues)

## Next releases

- **[0.5.0: performance](https://github.com/guizmaii-opensource/zazr/milestone/6).** Faster operations, with the same
  behaviour: tree deletion without temporary objects, `keySet()` without rebuilding, faster `combinations` and
  `permutations`, a real-time `Queue`, and operations that change nothing giving back the collection itself.
- **[0.6.0: warnings and sequence operations](https://github.com/guizmaii-opensource/zazr/milestone/7).** A
  `Validation` that also collects warnings, which never block the result, and sequence operations such as
  `zipWithPrevious`, `mapAccum` and `foldWhile`.
- **[Benchmarks](https://github.com/guizmaii-opensource/zazr/milestone/3).** Measurements of the collections and the
  JSON modules, published as they are done.

## Released

- **[0.4.0](https://github.com/guizmaii-opensource/zazr/releases/tag/v0.4.0).** More of Scala's collections:
  `groupMap`, `groupMapReduce`, `sizeCompare`, `updateWith` on maps and range operations on sorted sets and maps. In
  `zazr-test`, generators for the non-empty sets and maps and for null values.
- **[0.3.0](https://github.com/guizmaii-opensource/zazr/releases/tag/v0.3.0).** Modules that read and write Zazr
  types with Jackson and with avaje-jsonb.
- **[0.2.0](https://github.com/guizmaii-opensource/zazr/releases/tag/v0.2.0).** `Option.unless`, `Option.reject` and
  `Either.merge`, and a `LazyList` that always prints, cycles included.
- **[0.1.0](https://github.com/guizmaii-opensource/zazr/releases/tag/v0.1.0).** The first release.

[GitHub Releases](https://github.com/guizmaii-opensource/zazr/releases) lists every release and what it changes.
