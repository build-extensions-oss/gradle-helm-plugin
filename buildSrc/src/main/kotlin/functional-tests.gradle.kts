plugins {
    id("kotlin-convention")
}


val functionalTest: SourceSet by sourceSets.creating

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath

    // needs to be removed via https://github.com/build-extensions-oss/gradle-helm-plugin/issues/41
    val urlOverrideProperty = "io.github.build.extensions.oss.gradle.helm.plugin.distribution.url.prefix"
    findProperty(urlOverrideProperty)?.let { urlOverride ->
        systemProperty(urlOverrideProperty, urlOverride)
    }

    // in simplified run, we will re-publish everything into maven local before running functional tests
    if (!cleanRunEnabled) {
        // before running functional test - we must publish all plugins locally
        dependsOn(tasks.named("publishAllPublicationsToLocalRepoRepository"))
        // depend on main plugin as well - otherwise recompilation might not happen
        dependsOn(project(":helm-plugin").tasks.named("publishAllPublicationsToLocalRepoRepository"))
    }

    doFirst {
        val runOnGitHub = providers.environmentVariable("GITHUB_ACTIONS")
            .map { it.toBoolean() }
            .orElse(false)
            .get()

        if (runOnGitHub && !cleanRunEnabled) {
            throw GradleException("functionalTests are requested to run on GitHub, however property ${BuildConstants.FUNCTIONAL_TESTS_ONLY} wasn't set. Please set up it for explicit configuration.")
        }
    }
    // ensure that we have all files required
    doFirst {
        val localRepositoryFolder =
            rootProject.layout.buildDirectory.dir("local-repo/io/github/build-extensions-oss").get().asFile

        require(localRepositoryFolder.isDirectory) {
            "$localRepositoryFolder is not a directory: it must be presented on disk before run"
        }
    }
}

/**
 * Copies all Kover coverage files into a single directory, which will be uploaded by GitHub
 */
tasks.register<Copy>("copyFunctionalTestCoverage") {
    from(layout.buildDirectory.dir("kover/bin-reports"))
    include("**/*.ic")
    // rename from 'functionalTest.ic' to 'functionalTest-<project.name>.ic' to avoid same files rewriting
    rename {
        val oldPrefix = it.substringBeforeLast(".ic")
        val intermediate = project.name
        "$oldPrefix-$intermediate.ic"
    }
    // see notation './build/tmp/collected/kover-bin-reports/' in pre-merge.yaml
    into(rootProject.layout.buildDirectory.dir("tmp/collected/kover-bin-reports/"))

    dependsOn(functionalTestTask)
}

