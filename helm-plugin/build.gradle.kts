plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
    alias(libs.plugins.detekt)
    alias(libs.plugins.binaryCompatibilityValidator)
    id("kotlin-convention") // keep shared logic here
}


dependencies {

    implementation(libs.snakeyaml)
    implementation(libs.orgJson)

    //implementation(libs.unbrokenDomePluginUtils)
    implementation(files("../../gradle-plugin-utils/gradle-plugin-utils/build/libs/gradle-plugin-utils-0.0.1.jar"))

    testImplementation(libs.jsonPath)
    testImplementation(libs.jacksonDataBind)
    testImplementation(libs.jacksonDataFormatYaml)

    testImplementation(libs.okHttpMockWebServer)

    testImplementation(libs.coroutinesCore)

    //testImplementation(libs.unbrokenDomeTestUtils)
    testImplementation(files("../../gradle-plugin-utils/gradle-plugin-test-utils/build/libs/gradle-plugin-test-utils-0.0.1.jar"))

    testImplementation(libs.bundles.defaultTests)
    testRuntimeOnly(libs.junitEngine)
}


gradlePlugin {
    plugins {
        create("helmCommandsPlugin") {
            id = project.extra["plugin.prefix"].toString() + ".helm-commands"
            displayName = "Helm Commands"
            implementationClass = "com.citi.gradle.plugins.helm.command.HelmCommandsPlugin"
            description = "Wrapper for common helm commands"
            tags.addAll("helm", "helm commands", "kubernetes", "k8s", "cloud")
        }
        create("helmPlugin") {
            id = project.extra["plugin.prefix"].toString() + ".helm"
            displayName = "Helm"
            implementationClass = "com.citi.gradle.plugins.helm.HelmPlugin"
            description = "Gradle plugin to help preparing Helm Charts. Supports charts packaging, linting, dependencies update, etc."
            tags.addAll("helm", "package", "kubernetes", "k8s", "cloud", "repository", "lint")
        }
    }
}

apiValidation {
    ignoredPackages.add("com.citi.gradle.plugins.helm.dsl.internal")
}