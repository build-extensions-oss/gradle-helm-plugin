plugins {
    id("kotlin-convention") // keep shared logic here
}

dependencies {
    api(gradleTestKit())
    api(libs.bundles.defaultTests)
    runtimeOnly(libs.bundles.defaultTestsRuntime)

    // import code coverage from other plugins - basically, some code from this project is used there
    kover(project(":helm-plugin"))
    kover(project(":helm-publish-plugin"))
    kover(project(":helm-releases-plugin"))
}