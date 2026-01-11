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
// The lowest Java supported by Gradle 9 is Java 17 - let's keep it as a baseline.
val projectJvmTarget = JvmTarget.JVM_17
val projectJvmTargetInt = 17

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

val externalArtifactsFolder = rootProject.layout.buildDirectory.dir(
    "tmp/kover-artifacts"
).get()

val externalCoverageFiles = externalArtifactsFolder.asFileTree.matching { include("**/*.ic") }

kover {
    currentProject {
        sources {
            excludedSourceSets.add("test")
        }
    }
    reports {
        // this setting is propagated to all child projects. They will apply these code coverage results on top on what they have
        total {
            additionalBinaryReports.addAll(externalCoverageFiles)
        }
    }
}

tasks.register("validateExternalKoverArtifacts") {
    doLast {
        require(externalCoverageFiles.files.isNotEmpty()) {
            "Unable to find any file in external artifacts in ${externalArtifactsFolder}: ${externalCoverageFiles.files.joinToString()}"
        }
    }
}

// if someone request build task - let's configure it additionally
rootProject.tasks.named("build").configure {
    tasks {
        // prohibit build without verification
        dependsOn(named("koverCachedVerify"))
        // prohibit build without running detekt
        // uncomment after https://github.com/build-extensions-oss/gradle-helm-plugin/issues/65
        // dependsOn(named("detektMain"))
        // dependsOn(named("detektTest"))
    }
}

// fix after https://github.com/build-extensions-oss/gradle-helm-plugin/issues/65
tasks.getByName("detekt") {
    enabled = false
}