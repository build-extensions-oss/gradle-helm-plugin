/**
 * Collect here all plugins which we need to apply for a project which represents Gradle plugin.
 */
plugins {
    id("kotlin-convention")
    id("dokka-convention")
    // is needed for local publications only
    id("maven-publish")
}

// from https://docs.gradle.org/current/userguide/part8_publish_locally.html
/**
 * We publish all plugins into the same folder, so we will be able to use jars in functional tests.
 * That allows us to avoid recompilation on each platform.
 * So, in GitHub actions, instead of "compile on Java X - test on Java X" we use logic "compile once on minimal java - test on Java X.
 */
extensions.configure<PublishingExtension>("publishing") {
    // run task "publishAllPublicationsToLocalRepoRepository"
    repositories {
        maven {
            name = "localRepo"
            url = rootProject.layout.buildDirectory.dir("local-repo").get().asFile.toURI()
        }
    }
}