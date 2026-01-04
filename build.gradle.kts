plugins {
    alias(libs.plugins.gradlePublish) apply false
    id("org.jetbrains.dokka")
    alias(libs.plugins.asciidoctor)
    alias(libs.plugins.benManesVersions)
    // is defined in buildSrc
    id("org.jetbrains.kotlinx.kover")
}


allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaGradlePluginPlugin> {
        dependencies {
            "compileOnly"(kotlin("stdlib"))
        }

        with(the<GradlePluginDevelopmentExtension>()) {
            isAutomatedPublishing = true
        }

        with(the<JavaPluginExtension>()) {
            withSourcesJar()
            withJavadocJar()
        }
    }


    plugins.withId("org.jetbrains.kotlin.jvm") {
        configurations.all {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    useVersion(libs.versions.kotlin.get())
                }
            }
        }

        dependencies {
            "testImplementation"(kotlin("stdlib"))
            "testImplementation"(kotlin("reflect"))

            "testImplementation"(libs.assertk)
            "testImplementation"(libs.mockk)
            "testImplementation"(libs.spekDsl)
            "testRuntimeOnly"(libs.spekRunner)
        }
    }

    plugins.withId("com.gradle.plugin-publish") {
        val githubUrl = project.extra["github.url"] as String

        with(the<GradlePluginDevelopmentExtension>()) {
            website.set(githubUrl)
            vcsUrl.set(githubUrl)
            description = "A suite of Gradle plugins for building, publishing and managing Helm charts."
            plugins.forEach { plugin ->
                plugin.tags.add("helm")
            }
        }
    }
}

tasks {
    this.getByName("build") {
        // prohibit building without verification
        dependsOn(getByName("koverCachedVerify"))
    }
}

val externalCoverageFiles = rootProject.layout.buildDirectory.dir(
    "tmp/kover-artefacts"
).get().asFileTree.matching { include("**/*.ic") }

kover.reports {
    verify {
        rule {
            bound {
                minValue.set(14)
            }
        }
    }

    // this setting is propagated to all child projects. They will apply these code coverage results on top on what they have
    total {
        additionalBinaryReports.addAll(externalCoverageFiles)
    }
}

tasks.register("validateExternalKoverArtifacts") {
    doLast {
        require(externalCoverageFiles.files.isNotEmpty()) {
            "Unable to find any file in external artifacts: ${externalCoverageFiles.files.joinToString()}"
        }
    }
}

val asciidoctorExt: Configuration by configurations.creating

dependencies {
    asciidoctorExt(libs.tabbedCodeExtension)
}


tasks.named("asciidoctor", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {

    sourceDir("docs")
    baseDirFollowsSourceDir()
    sources {
        include("index.adoc")
    }

    configurations(asciidoctorExt.name)

    options(
        mapOf(
            "doctype" to "book"
        )
    )
    attributes(
        mapOf(
            "project-version" to project.version,
            "source-highlighter" to "prettify"
        )
    )
}
