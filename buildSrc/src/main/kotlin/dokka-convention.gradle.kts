import org.jetbrains.dokka.gradle.DokkaExtension

plugins {
    id("org.jetbrains.dokka")
}

// have an option to disable Dokka task for local builds
tasks.withType<Jar>().matching { it.name == "javadocJar" || it.name == "publishPluginJavaDocsJar" }
    .all {
        from(tasks.named("dokkaJavadoc"))
    }


// strangely, "dokka {}" doesn't work here for Gradle 8. Worth migration later...
extensions.configure<DokkaExtension>("dokka") {
    val sourceSetName = "main"
    dokkaSourceSets.getByName(sourceSetName) {
        // this documentation is used in our project. Let's register everything to provide smooth links
        val externalLinkNameToUrl = listOf(
            "java" to "https://docs.oracle.com/javase/17/docs/api/",
            "javadoc" to "https://docs.gradle.org/current/javadoc/",
            "groovy" to "https://docs.groovy-lang.org/latest/html/groovy-jdk/",
        )

        externalDocumentationLinks {
            externalLinkNameToUrl.forEach { (linkName, docUrl) ->
                register(linkName) {
                    url.set(uri(docUrl))
                }
            }
        }

        reportUndocumented.set(false)

        val githubUrl = project.extra["github.url"] as String

        sourceLink {
            localDirectory.set(project.file("src/$sourceSetName/kotlin"))
            remoteUrl.set(
                uri("$githubUrl/blob/v${project.version}/${project.projectDir.relativeTo(rootDir)}/src/$sourceSetName/kotlin")
            )
            remoteLineSuffix.set("#L")
        }
    }
}