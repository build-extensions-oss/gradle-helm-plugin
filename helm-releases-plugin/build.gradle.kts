plugins {
    id("gradle-plugin-convention") // keep shared logic here
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    alias(libs.plugins.binaryCompatibilityValidator)
}


dependencies {

    implementation(project(":helm-plugin"))
    testImplementation(project(":unit-test-utils"))

    implementation(libs.io.github.build.extensions.oss.gradle.plugin.utils)
}


gradlePlugin {
    plugins {
        create("helmReleasesPlugin") {
            id = project.extra["plugin.prefix"].toString() + ".helm-releases"
            displayName = "Helm Releases"
            implementationClass = "io.github.build.extensions.oss.gradle.plugins.helm.release.HelmReleasesPlugin"
            description = "Extension for Gradle Helm Plugin. Supports charts installation/uninstallation."
            tags.addAll("helm", "release", "install", "uninstall", "cloud", "kubernetes")
        }
    }
}
dokka {
    moduleName.set("Gradle Helm Releases Plugin")
}