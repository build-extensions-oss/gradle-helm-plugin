plugins {
    id("gradle-plugin-convention") // keep shared logic here
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
    alias(libs.plugins.binaryCompatibilityValidator)
    id("functional-tests") // logic related to Gradle functional testing is here
}

dependencies {
    implementation(project(":helm-plugin"))

    implementation(libs.okHttp) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }
    implementation(libs.okHttpTls) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }

    implementation(libs.io.github.build.extensions.oss.gradle.plugin.utils)

    testImplementation(project(":unit-test-utils"))

    "functionalTestImplementation"(project(":plugin-test-utils"))
    "functionalTestImplementation"(libs.okHttpMockWebServer)
    "functionalTestRuntimeOnly"(libs.bundles.defaultTestsRuntime)
}


gradlePlugin {
    testSourceSets(sourceSets.test.get(), sourceSets["functionalTest"])
    plugins {
        create("helmPublishPlugin") {
            id = project.extra["plugin.prefix"].toString() + ".helm-publish"
            displayName = "Helm Publish"
            implementationClass = "io.github.build.extensions.oss.gradle.plugins.helm.publishing.HelmPublishPlugin"
            description =
                "Extension for Gradle Helm Plugin. Allows helm chart publishing. Helm doesn't have this feature, so different publications are used for different helm repository providers"
            tags.addAll("helm", "publish")
        }
    }
}

apiValidation {
    ignoredPackages.add("io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.internal")
}

dokka {
    moduleName.set("Gradle Helm Publish Plugin")
}