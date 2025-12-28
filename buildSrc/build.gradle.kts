plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.io.gitlab.arturbosch.detekt.detekt.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.kotlin.gradle.plugin)
    implementation(libs.org.jetbrains.dokka.org.jetbrains.dokka.gradle.plugin)
    implementation(libs.org.jetbrains.dokka.javadoc.org.jetbrains.dokka.javadoc.gradle.plugin)
    implementation(libs.org.jetbrains.kotlinx.kover)
}