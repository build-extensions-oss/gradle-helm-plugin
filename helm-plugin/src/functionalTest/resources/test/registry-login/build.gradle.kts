/**
 * Kotlin DSL on purpose: this fixture reproduces a consumer build whose chart pulls a dependency straight from
 * an OCI registry named only in Chart.yaml - nothing is declared in `helm.repositories` - and which used to log
 * in with `doFirst { helm.execHelm("registry", "login") { ... } }` on helmUpdateMainChartDependencies.
 * That lambda captures the build script, and with it the Project, which the configuration cache cannot store.
 *
 * Every property uses `.set(...)` rather than assignment so the fixture runs on the whole Gradle matrix.
 */
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmRegistryLogin

plugins {
    id("io.github.build-extensions-oss.helm") version "0.0.1"
}

helm {
    registries {
        // Chart.yaml pulls a dependency from this one, so the dependency tasks log in to it.
        create("artifactory") {
            host.set("registry.example.com")
            credentials {
                username.set(providers.gradleProperty("registryUser"))
                password.set(
                    providers.environmentVariable("REGISTRY_PASSWORD")
                        .orElse(providers.gradleProperty("registryPassword"))
                )
            }
        }

        // No chart dependency comes from this one, so nothing may log in to it.
        create("unused") {
            host.set("unused.example.com")
            credentials {
                username.set("unused-user")
                password.set("unused-password")
            }
        }
    }

    charts {
        create("main") {
            sourceDir.set(file("src/main/helm"))
        }
    }
}

// An ad-hoc login task: it must not be wired to the dependency tasks, which log in by themselves.
tasks.register<HelmRegistryLogin>("helmRegistryLogin") {
    registry.set("adhoc.example.com")
    username.set(providers.gradleProperty("registryUser"))
    password.set(providers.gradleProperty("registryPassword"))
}
