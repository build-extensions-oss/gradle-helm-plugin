package io.github.build.extensions.oss.gradle.plugins.helm

import io.kotest.matchers.string.shouldContain
import java.io.File
import java.util.Properties
import org.junit.jupiter.api.Test

class PluginVersionSynchronization {

    /**
     * This verification is needed before https://github.com/Citi/gradle-helm-plugin/issues/24 is implemented to have the same plugin version with documentation
     */
    @Test
    fun pluginVersionShouldBeTheSameWithDocumentation() {
        val pluginVersion = getPluginVersion()
        val expectedDocumentationLine = "$pluginVersion"

        val readmeFile = File("../README.md").absoluteFile.readText()

        readmeFile shouldContain expectedDocumentationLine
    }

    private fun getPluginVersion(): Version {
        val gradleProperties = Properties().apply {
            File("../gradle.properties").absoluteFile.reader().use { fileReader ->
                load(fileReader)
            }
        }

        val pluginVersion = gradleProperties.getProperty("version")

        return Version.parse(pluginVersion)
    }

    private data class Version(val major: Int, val minor: Int, val revision: Int) {
        companion object {
            fun parse(input: String): Version {
                val fragments = input.split('.')

                require(fragments.size == 3) {
                    "Fragment count should be 3, but was ${fragments.size}: $input"
                }

                return Version(fragments[0].toInt(), fragments[1].toInt(), fragments[2].toInt())
            }
        }

        override fun toString(): String {
            return "$major.$minor.$revision"
        }
    }
}