plugins {
    `java-library`
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
    id("dependencies-lock")
    id("org.jetbrains.kotlinx.kover")
}

/*
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}*/

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