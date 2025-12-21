import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
    id("dependencies-lock")
    id("org.jetbrains.kotlinx.kover")
}

// let's keep JVM versions in a one place
val projectJvmTarget = JvmTarget.JVM_1_8
val projectJvmTargetInt = 8

configure<KotlinJvmProjectExtension> {
    jvmToolchain(projectJvmTargetInt)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget = projectJvmTarget
    }
}
tasks.withType<JavaCompile>().configureEach {
    // synchronize Java version with Kotlin compiler
    options.release.set(projectJvmTargetInt)
}

tasks.withType<Test> {
    // always execute tests
    outputs.upToDateWhen { false }

    useJUnitPlatform()

    testLogging.showStandardStreams = true

    // give tests a temporary directory below the build dir so
    // we don't pollute the system temp dir (Gradle tests don't clean up)
    systemProperty("java.io.tmpdir", layout.buildDirectory.dir("tmp").get())

    maxParallelForks = (project.property("test.maxParallelForks") as String).toInt()
    if (maxParallelForks > 1) {
        // Parallel tests seem to need a little more time to set up, so increase the test timeout to
        // make sure that the first test in a forked process doesn't fail because of this
        systemProperty("SPEK_TIMEOUT", 30000)
    }
}

tasks.jar {
    archiveVersion.set(project.version.toString())
}

// otherwise Gradle fails with 'Cannot publish artifacts as no version set.'
project.version = rootProject.version.toString()

kover {
    currentProject {
        sources {
            excludedSourceSets.add("test")
        }
    }
}

// if someone request build task - let's configure it additionally
rootProject.tasks.named("build").configure {
    tasks {
        // prohibit build without verification
        dependsOn(named("koverCachedVerify"))
        // prohibit build without running detekt
        dependsOn(named("detektMain"))
        dependsOn(named("detektTest"))
    }
}