plugins {
    `java-library`
    id("dependencies-lock")
    id("org.jetbrains.kotlinx.kover")
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