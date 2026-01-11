![Documentation](https://img.shields.io/badge/documentation?link=https%3A%2F%2Fbuild-extensions-oss.github.io%2Fgradle-helm-plugin%2F)
![Gradle Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.build-extensions-oss.helm?colorA=0f1632&colorB=255be3)
![Licence](https://img.shields.io/github/license/build-extensions-oss/gradle-helm-plugin?label=license&colorA=0f1632&colorB=255be3)
![Scorecard](https://img.shields.io/ossf-scorecard/github.com/build-extensions-oss/gradle-helm-plugin?label=openssf+scorecard&style=flat)
[![codecov](https://codecov.io/gh/build-extensions-oss/gradle-helm-plugin/graph/badge.svg?token=7Q2YBFQRCG)](https://codecov.io/gh/build-extensions-oss/gradle-helm-plugin)

## Features

- Gradle task types for common Helm CLI commands

- Build, package and publish Helm Charts using a declarative Gradle DSL

- Resolve placeholders like ${chartVersion} from chart source files before packaging

- Resolve dependencies between charts using Gradle artifact dependencies

- Install, upgrade and uninstall releases to/from a Kubernetes cluster

## Quick Start

Add `io.github.build-extensions-oss.helm` to your Gradle project:

```gradle
plugins {
    id 'io.github.build-extensions-oss.helm' version 'latest'
}
```

```gradle
plugins {
    id("io.github.build-extensions-oss.helm") version "latest"
}
```

```gradle
📂 (project root)
    📂 src
        📂 main
            📂 helm
                📂 templates
                    📄 ...
                📄 Chart.yaml
                📄 values.yaml
```

## Migration from older versions

This repository is a fork of [Citi/gradle-helm-plugin](https://github.com/Citi/gradle-helm-plugin), which is a
of [unbroken-dome/gradle-helm-plugin](https://github.com/unbroken-dome/gradle-helm-plugin).

The version [v2.2.0](https://github.com/build-extensions-oss/gradle-helm-plugin/releases/tag/v2.2.0) has exactly the
same code
with version [2.2.0](https://github.com/Citi/gradle-helm-plugin/releases/tag/2.2.0)
of [Citi/gradle-helm-plugin](https://github.com/Citi/gradle-helm-plugin). Therefore, first please use that version. All
Java/Kotlin packages are the same, so the plugin should be fully compatible.

Version `3.1.0` (under construction) might be not backward compatible with older plugin versions, because:

* It has new Java/Kotlin packages that match this project.
    * Unless you call plugin functions directly, it would be hard to notice any differences with simple plugin usages
* It is compatible with Gradle 9 (and remain being compatible with Gradle 7), however some old APIs have been removed.
* The minimal Java version is Java 17 (not 1.8).

## Requirements

- Gradle 7 or higher

- JDK 17 or higher (for running Gradle)

- Helm command-line client 3.+

### Limitations

Due to open source nature of the project, some functionality isn't supported for old versions of Gradle.

| Feature                        | Gradle Version                                                                 |
|--------------------------------|--------------------------------------------------------------------------------|
| Basic Helm Chart Functionality | 7, 8, 9                                                                        |
| Helm Publish                   | 7, 8, 9                                                                        |
| Helm Releases                  | 7, 8 - basic support (not tested regularly); 9 - full support (constant tests) |

## Contributing

Your contributions are at the core of making this a true open source project. Any contributions you make are **greatly
appreciated**.

We welcome you to:

- Fix typos or touch up documentation
- Share your opinions on [existing issues](https://github.com/build-extensions-oss/gradle-helm-plugin/issues)
- Help expand and improve our library
  by [opening a new issue](https://github.com/build-extensions-oss/gradle-helm-plugin/issues/new)

Please review our [functional contribution guidelines](./CONTRIBUTING.md) to get started 👍

## Testing

Code is tested on GitHub. Additionally, code is manually verified via neighbour
repository: https://github.com/build-extensions-oss/gradle-plugin-examples.

There are three types of tests:

* Unit tests (with mocks and so on)
* Functional tests via Gradle simulation (old - should be removed). They run on Linux only. They are executed in the
  same time with unit tests.
* Functional tests via Gradle Test Kit.

The most comprehensive checks are done via Gradle Functional tests. And GitHub workflow logic is the following:

1. We compile code on ubuntu-latest and publish jars locally.
2. Plugin is published into local jar versioned with git hash (to avoid accidental usage from another build).
3. Functional tests:
    1. Executed on multiple platforms
    2. Use the jar published (e.g. the don't recompile the same plugin code)
    3. Run on different Java versions.
4. The latest GitHub action - we run all unit tests, however code coverage is aggregated with functional tests as well.

In other words, we compile code once and then run tests on different platforms. That is needed to check that the real
code will be good enough to be used on different operating systems.

## License

This project is distributed under the [MIT License](https://opensource.org/license/mit). See [`LICENSE`](./LICENSE) for
more information.
