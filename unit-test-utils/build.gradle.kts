plugins {
    id("kotlin-convention") // keep shared logic here
}

dependencies {
    api(libs.bundles.defaultTests)
    runtimeOnly(libs.bundles.defaultTestsRuntime)
}

kover {
    currentProject {
        sources {
            // exclude all source sets - this is purely test project
            excludedSourceSets.addAll("main", "test")
        }
    }
}