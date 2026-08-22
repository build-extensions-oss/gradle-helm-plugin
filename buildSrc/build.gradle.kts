plugins {
    `kotlin-dsl`
}

repositories {
    // ban default repositories - including Maven Central - to avoid error ' Received status code 429 from server: Too Many Requests'
    // https://github.com/renovatebot/renovate/discussions/43146
    // clear()

    gradlePluginPortal()
}

dependencies {
    implementation(libs.io.gitlab.arturbosch.detekt.detekt.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.kotlin.gradle.plugin)
    implementation(libs.org.jetbrains.dokka.org.jetbrains.dokka.gradle.plugin)
    implementation(libs.org.jetbrains.dokka.javadoc.org.jetbrains.dokka.javadoc.gradle.plugin)
    implementation(libs.org.jetbrains.kotlinx.kover)
}