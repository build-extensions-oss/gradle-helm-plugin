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
        with(the<GradlePluginDevelopmentExtension>()) {
            website = BuildConstants.GITHUB_PAGES_DOCUMENTATION
            vcsUrl = BuildConstants.GITHUB_URL
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

kover.reports {
    verify {
        rule {
            bound {
                minValue.set(14)
            }
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
