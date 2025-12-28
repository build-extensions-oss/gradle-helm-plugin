plugins {
    id("org.jetbrains.dokka")
}

// have an option to disable Dokka task for local builds
tasks.withType<Jar>().matching { it.name == "javadocJar" || it.name == "publishPluginJavaDocsJar" }
    .all {
        from(tasks.named("dokkaJavadoc"))
    }

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
    dokkaSourceSets.all {
        externalDocumentationLink {
            url.set(uri("https://docs.oracle.com/javase/8/docs/api/").toURL())
        }
        reportUndocumented.set(false)

        val sourceSetName = this.name
        val githubUrl = project.extra["github.url"] as String

        sourceLink {
            localDirectory.set(project.file("src/$sourceSetName/kotlin"))
            remoteUrl.set(
                uri("$githubUrl/blob/v${project.version}/${project.projectDir.relativeTo(rootDir)}/src/$sourceSetName/kotlin").toURL()
            )
            remoteLineSuffix.set("#L")
        }
    }
}


plugins.withType<JavaGradlePluginPlugin> {
    tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
        dokkaSourceSets.all {
            externalDocumentationLink {
                url.set(uri("https://docs.gradle.org/current/javadoc/").toURL())
            }
            externalDocumentationLink {
                url.set(uri("https://docs.groovy-lang.org/latest/html/groovy-jdk/").toURL())
            }
        }
    }
}