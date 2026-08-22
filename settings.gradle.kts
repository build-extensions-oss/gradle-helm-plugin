// don't uncomment the statements below and especially - the line 'mavenCentral' due to https://github.com/renovatebot/renovate/discussions/43146#discussioncomment-16917760
//pluginManagement {
//    repositories {
//        gradlePluginPortal()
//        mavenCentral() <<<<<<<<<<<< don't add this
//    }
//}

rootProject.name = "gradle-helm-plugin-parent"

include(
    "helm-plugin",
    "helm-publish-plugin",
    "helm-releases-plugin",
    "plugin-test-utils",
    "unit-test-utils",
)
