pluginManagement {
    repositories {
        maven {
            url = file("../../../../build/local-repo").toURI()
        }
        gradlePluginPortal()
    }
}
