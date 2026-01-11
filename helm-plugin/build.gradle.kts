plugins {
    id("gradle-plugin-convention") // keep shared logic here
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    alias(libs.plugins.binaryCompatibilityValidator)
}


dependencies {

    implementation(libs.snakeyaml)
    implementation(libs.orgJson)

    implementation(libs.io.github.build.extensions.oss.gradle.plugin.utils)

    testImplementation(libs.jsonPath)
    testImplementation(libs.jacksonDataBind)
    testImplementation(libs.jacksonDataFormatYaml)

    testImplementation(project(":unit-test-utils"))
    testImplementation(libs.okHttpMockWebServer)

    // import code coverage from other plugins - basically, some code from this project is used there
    kover(project(":helm-publish-plugin"))
    kover(project(":helm-releases-plugin"))
}

gradlePlugin {
    plugins {
        create("helmCommandsPlugin") {
            id = project.extra["plugin.prefix"].toString() + ".helm-commands"
            displayName = "Helm Commands"
            implementationClass = "io.github.build.extensions.oss.gradle.plugins.helm.command.HelmCommandsPlugin"
            description = "Wrapper for common helm commands"
            tags.addAll("helm", "helm commands", "kubernetes", "k8s", "cloud")
        }
        create("helmPlugin") {
            id = project.extra["plugin.prefix"].toString() + ".helm"
            displayName = "Helm"
            implementationClass = "io.github.build.extensions.oss.gradle.plugins.helm.HelmPlugin"
            description = "Gradle plugin to help preparing Helm Charts. Supports charts packaging, linting, dependencies update, etc."
            tags.addAll("helm", "package", "kubernetes", "k8s", "cloud", "repository", "lint")
        }
    }
}

apiValidation {
    ignoredPackages.add("io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal")
}

dokka {
    moduleName.set("Gradle Helm Plugin")
}