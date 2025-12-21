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

Version `3.1.0` (under construction) is not backward compatible with older plugin versions, because:

* It has new Java/Kotlin packages that match this project.
* It is compatible with Gradle 9 (and not with Gradle 7), and all old APIs have been removed.
* The minimal Java version is Java 17 (not 1.8).

## Requirements

- Gradle 7 or higher

- JDK 1.8 or higher (for running Gradle)

- Helm command-line client 3.+

## Contributing

Your contributions are at the core of making this a true open source project. Any contributions you make are **greatly
appreciated**.

We welcome you to:

- Fix typos or touch up documentation
- Share your opinions on [existing issues](https://github.com/build-extensions-oss/gradle-helm-plugin/issues)
- Help expand and improve our library by [opening a new issue](https://github.com/build-extensions-oss/gradle-helm-plugin/issues/new)

Please review our [functional contribution guidelines](./CONTRIBUTING.md) to get started 👍

## License

This project is distributed under the [MIT License](https://opensource.org/license/mit). See [`LICENSE`](./LICENSE) for
more information.
