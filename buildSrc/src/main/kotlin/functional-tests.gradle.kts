plugins {
    id("kotlin-convention")
}


val functionalTest: SourceSet by sourceSets.creating

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    mustRunAfter(tasks.test)

    // needs to be removed via https://github.com/build-extensions-oss/gradle-helm-plugin/issues/41
    val urlOverrideProperty = "io.github.build.extensions.oss.gradle.helm.plugin.distribution.url.prefix"
    findProperty(urlOverrideProperty)?.let { urlOverride ->
        systemProperty(urlOverrideProperty, urlOverride)
    }
}

tasks.build {
    dependsOn(functionalTestTask)
}

