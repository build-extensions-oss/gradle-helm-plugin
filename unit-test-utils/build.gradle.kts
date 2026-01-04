plugins {
    id("kotlin-convention") // keep shared logic here
}

/**
 * All these dependencies will be propagated to all test projects
 */
dependencies {
    api(libs.bundles.defaultTests)
    api(libs.io.github.build.extensions.oss.gradle.plugin.test.utils)
    runtimeOnly(libs.bundles.defaultTestsRuntime)
}

kover {
    currentProject {
        sources {
            // exclude all source sets - this is purely test project
            excludedSourceSets.addAll("main", "test")
        }
    }
}