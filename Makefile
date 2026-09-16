# Common tasks. `make` or `make help` lists them.
# Every target wraps ./mvnw so nobody has to remember Maven phases, profiles or plugin goals.

MVN := ./mvnw -B
TEST ?=
MODULE ?=
PL := $(if $(MODULE),-pl $(MODULE) -am,)

.DEFAULT_GOAL := help

.PHONY: help clean compile test-compile test test-one package install verify fmt fmt-check nullness bench javadoc generate deps-updates

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

verify: ## what CI runs: full build with tests, formatting and nullness checks
	$(MVN) verify
	$(MVN) -Pnullaway compile

fmt: ## format the sources (spotless apply)
	$(MVN) spotless:apply

fmt-check: ## fail if sources are not formatted (spotless check)
	$(MVN) spotless:check

nullness: ## NullAway / JSpecify nullness check
	$(MVN) -Pnullaway compile

bench: ## run the JMH benchmarks (com.guizmaii.zazr.JmhRunner, zazr-benchmark module)
	$(MVN) -Pbenchmark -pl zazr-benchmark -am -DskipTests test

javadoc: ## build the javadoc (doclint)
	$(MVN) javadoc:javadoc

deps-updates: ## list newer versions of dependencies and plugins
	$(MVN) versions:display-dependency-updates versions:display-plugin-updates
