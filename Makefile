# Common tasks. `make` or `make help` lists them.
# Every target wraps ./mvnw so nobody has to remember Maven phases, profiles or plugin goals.

MVN := ./mvnw -B
TEST ?=
MODULE ?=
PL := $(if $(MODULE),-pl $(MODULE) -am,)

.DEFAULT_GOAL := help

.PHONY: help clean compile test-compile test test-one package install verify fmt fmt-check nullness vocabulary complexity docs-complexity docs-complexity-check docs-examples site site-serve bench coverage coverage-summary javadoc generate deps-updates

help: ## list the targets
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

clean: ## delete target/ and the generated sources
	$(MVN) clean

generate: ## regenerate src-gen from generator/Generator.scala
	$(MVN) generate-sources

compile: ## compile main sources (runs the generator first)
	$(MVN) compile

test-compile: ## compile main and test sources
	$(MVN) test-compile

test: ## run the whole test suite
	$(MVN) test

test-one: ## run one test class or method: make test-one TEST=VectorTest [MODULE=zazr-core]  |  TEST='VectorTest#shouldAppend*'
	@test -n "$(TEST)" || { echo "usage: make test-one TEST=ClassName[#method] [MODULE=zazr-core]"; exit 1; }
	$(MVN) $(PL) test -Dtest='$(TEST)' -Dsurefire.failIfNoSpecifiedTests=false

package: ## build the jars (runs tests)
	$(MVN) package

install: ## install the jars into ~/.m2 (runs tests)
	$(MVN) install

verify: ## what CI runs: full build with tests, formatting, nullness, javadoc, vocabulary, complexity and docs checks
	$(MVN) verify
	$(MVN) -Pnullaway compile
	$(MAKE) javadoc
	$(MAKE) vocabulary
	$(MAKE) complexity
	$(MAKE) docs-complexity-check
	$(MAKE) docs-examples

vocabulary: ## fail on category-theory vocabulary outside docs/design.md (CLAUDE.md: use the ZIO names)
	@hits="$$(git grep -n -i --untracked -E 'monad|functor|applicative|semigroup|monoid' -- zazr-core zazr-test zazr-benchmark docs ':!docs/design.md')"; \
	if [ -n "$$hits" ]; then echo "$$hits"; echo "category-theory vocabulary found; use the ZIO names (see CLAUDE.md)"; exit 1; fi

# The files whose positional and size-sensitive methods must document their cost (design.md 3.7), and the
# supertypes read to resolve the notes a type inherits.
COMPLEXITY_DIR := zazr-core/src/main/java/com/guizmaii/zazr/collection
COMPLEXITY_FILES := $(addprefix $(COMPLEXITY_DIR)/, \
	Vector.java List.java Queue.java Stream.java NonEmptyVector.java \
	HashSet.java LinkedHashSet.java TreeSet.java SortedSet.java \
	HashMap.java LinkedHashMap.java TreeMap.java SortedMap.java)
COMPLEXITY_CONTEXT := $(addprefix --context $(COMPLEXITY_DIR)/, Traversable.java Set.java Map.java)
COMPLEXITY_PAGE := docs/collections/complexity.md
COMPLEXITY_GLANCE := docs/collections/.costs

complexity: ## fail when a collection method lacks a "Complexity:" javadoc line or its class (design.md 3.7)
	@scala-cli run scripts/check-complexity.scala -- $(COMPLEXITY_CONTEXT) $(COMPLEXITY_FILES)

docs-complexity: ## regenerate docs/collections/complexity.md and the per-type tables from the "Complexity:" notes
	@scala-cli run scripts/check-complexity.scala -- $(COMPLEXITY_CONTEXT) --page $(COMPLEXITY_PAGE) --glance $(COMPLEXITY_GLANCE) $(COMPLEXITY_FILES)

docs-complexity-check: docs-complexity ## fail when the committed complexity page differs from a fresh generation
	@if [ -n "$$(git status --porcelain -- $(COMPLEXITY_PAGE) $(COMPLEXITY_GLANCE))" ]; then \
		git status --porcelain -- $(COMPLEXITY_PAGE) $(COMPLEXITY_GLANCE); \
		git --no-pager diff -- $(COMPLEXITY_PAGE) $(COMPLEXITY_GLANCE); \
		echo "the complexity page is stale or untracked: run make docs-complexity and commit the result"; exit 1; fi

# The tests that compile and run every fenced java block of the site (zazr-test's own, since zazr-core cannot depend on it).
DOCS_EXAMPLES_TESTS := \
	zazr-core/src/test/java/com/guizmaii/zazr/docs/DocsExamplesTest.java \
	zazr-test/src/test/java/com/guizmaii/zazr/test/docs/DocsTestingExamplesTest.java

docs-examples: ## fail when a java block of the site is not in a docs example test (they compile and run every snippet)
	@scala-cli run scripts/check-docs-examples.scala -- --docs docs --exclude docs/design.md $(DOCS_EXAMPLES_TESTS)

# The site (MkDocs + Material), built in a local virtualenv pinned by requirements-docs.txt.
DOCS_VENV := .venv-docs
DOCS_PYTHON ?= python3

$(DOCS_VENV)/.installed: requirements-docs.txt
	$(DOCS_PYTHON) -m venv $(DOCS_VENV)
	$(DOCS_VENV)/bin/pip install --quiet --upgrade pip
	$(DOCS_VENV)/bin/pip install --quiet -r requirements-docs.txt
	@touch $@

site: $(DOCS_VENV)/.installed ## build the website into site/ (mkdocs build --strict: fails on a broken link)
	$(DOCS_VENV)/bin/mkdocs build --strict

site-serve: $(DOCS_VENV)/.installed ## preview the website at http://127.0.0.1:8000/ with live reload
	$(DOCS_VENV)/bin/mkdocs serve --dev-addr 127.0.0.1:8000

fmt: ## format the sources (spotless apply)
	$(MVN) spotless:apply

fmt-check: ## fail if sources are not formatted (spotless check)
	$(MVN) spotless:check

nullness: ## NullAway / JSpecify nullness check
	$(MVN) -Pnullaway compile

bench: ## run the JMH benchmarks (com.guizmaii.zazr.JmhRunner, zazr-benchmark module)
	$(MVN) -Pbenchmark -pl zazr-benchmark -am -DskipTests test

# zazr-benchmark has no tests and stays out of the report.
COVERAGE_REPORT := zazr-test/target/site/jacoco-aggregate

coverage: ## test coverage of zazr-core and zazr-test (JaCoCo): HTML report in zazr-test/target/site/jacoco-aggregate
	$(MVN) -Pcoverage -pl zazr-core,zazr-test test
	@$(MAKE) --no-print-directory coverage-summary
	@echo "HTML report: $(COVERAGE_REPORT)/index.html"

coverage-summary: ## print the line and branch coverage per module and package of the last make coverage, in Markdown
	@scala-cli run scripts/coverage-summary.scala -- $(COVERAGE_REPORT)/jacoco.xml

# javadoc-no-fork after compile, not javadoc:javadoc: the forked lifecycle of javadoc:javadoc stops at generate-sources,
# so on a fresh checkout the plugin finds no module-info.class in zazr-core and refuses the named module.
# Output: <module>/target/reports/apidocs.
javadoc: ## build the javadoc of zazr-core and zazr-test (doclint: fails on a broken reference or malformed tag)
	$(MVN) compile javadoc:javadoc-no-fork

deps-updates: ## list newer versions of dependencies and plugins
	$(MVN) versions:display-dependency-updates versions:display-plugin-updates
