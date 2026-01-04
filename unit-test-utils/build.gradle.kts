plugins {
    id("kotlin-convention") // keep shared logic here
}

/**
 * All these dependencies will be propagated to all test projects
 */
dependencies {
    api(libs.junit)
    api(libs.kotestAssertions)

    api(libs.io.github.build.extensions.oss.gradle.plugin.test.utils)

    // both dependencies below are needed for running JUnit test with Gradle 9
    runtimeOnly(libs.junitEngine)
    runtimeOnly(libs.junitPlatform)
}

kover {
    currentProject {
        sources {
            // exclude all source sets - this is purely test project
            excludedSourceSets.addAll("main", "test")
        }
    }
}