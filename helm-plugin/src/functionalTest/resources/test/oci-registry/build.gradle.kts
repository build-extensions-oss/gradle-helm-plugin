/**
 * Kotlin DSL on purpose: this fixture reproduces a consumer build whose chart pulls a dependency from an
 * OCI registry that needs a login. It must not need any `doFirst { helm.execHelm("registry", "login") }`
 * workaround: such a script lambda captures the build script, and with it the Project, which the
 * configuration cache cannot store.
 *
 * Every property uses `.set(...)` rather than assignment so the fixture runs on the whole Gradle matrix.
 */
plugins {
    id("io.github.build-extensions-oss.helm") version "0.0.1"
}

helm {
    repositories {
        create("artifactory") {
            url("oci://registry.example.com:5000/helm-local")

            credentials {
                username.set(providers.gradleProperty("registryUser"))
                password.set(
                    providers.environmentVariable("REGISTRY_PASSWORD")
                        .orElse(providers.gradleProperty("registryPassword"))
                )
            }
        }
    }

    charts {
        create("main") {
            sourceDir.set(file("src/main/helm"))
        }
    }
}
