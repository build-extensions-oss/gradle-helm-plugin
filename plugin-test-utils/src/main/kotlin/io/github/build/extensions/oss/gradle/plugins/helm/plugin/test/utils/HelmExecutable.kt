package io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils

import java.io.File

/**
 * Helping class creating helm executable, which copies tgz file to a destination position.
 *
 * Is needed to simulate real helm (we don't need to verify this) and ignore actions done by other parts of a plugin.
 */
object HelmExecutable {
    private val helmExecutablesDirectory = File("./src/functionalTest/resources/executable")
    private val fileExtension = let {
        val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)

        if (isWindows) {
            "bat"
        } else {
            "sh"
        }
    }

    private val executableFileToCreateTgz = File(helmExecutablesDirectory, "helm-create-tgz.$fileExtension")

    private val noOpExecutableFile = File(helmExecutablesDirectory, "helm-noop.$fileExtension")

    private val recordingExecutableFile = File(helmExecutablesDirectory, "helm-record.$fileExtension")

    private val sourceTgzFile = File(helmExecutablesDirectory, "tgz-file-template.txt")

    /**
     * Function does multiple things:
     * 1. Prepares shell script which will copy tgz file in the temp folder
     * 2. Creates gradle parameter with path to that executable
     */
    fun getExecutableParameterForChartCreation(
        temporaryFolder: File,
        tgzFileDestination: File
    ): HelmExecutableParameter {
        val destinationExecutableFile = File(temporaryFolder, executableFileToCreateTgz.name)

        destinationExecutableFile.writer().use { writer ->
            executableFileToCreateTgz.forEachLine { line ->
                val newLine = line
                    .replace("%TGZ_SOURCE%", sourceTgzFile.normalize().absolutePath)
                    .replace("%TGZ_DESTINATION%", tgzFileDestination.normalize().absolutePath)

                writer.appendLine(newLine)
            }
        }

        destinationExecutableFile.setExecutable(true)

        return HelmExecutableParameter(destinationExecutableFile)
    }

    /**
     * Copies a do-nothing helm executable into the given folder, and creates the Gradle parameter which points
     * the plugin at it.
     *
     * The script exits successfully for every command, and answers `helm ls` with an empty JSON array. That is
     * enough for a build to run to completion without a Kubernetes cluster, which is what tests need when they
     * care about the build itself rather than about what helm does.
     */
    fun getNoOpExecutableParameter(temporaryFolder: File): HelmExecutableParameter {
        val destinationExecutableFile = File(temporaryFolder, noOpExecutableFile.name)

        noOpExecutableFile.copyTo(destinationExecutableFile, overwrite = true)
        destinationExecutableFile.setExecutable(true)

        return HelmExecutableParameter(destinationExecutableFile)
    }

    /**
     * Creates a helm executable which succeeds for every command and appends each invocation - its arguments,
     * followed by the `KUBECONFIG` it was given - as one line to [invocationLog].
     *
     * Use it when a test needs to know *how* helm was called, not just that the build ran.
     */
    fun getRecordingExecutableParameter(temporaryFolder: File, invocationLog: File): HelmExecutableParameter {
        val destinationExecutableFile = File(temporaryFolder, recordingExecutableFile.name)

        destinationExecutableFile.writer().use { writer ->
            recordingExecutableFile.forEachLine { line ->
                writer.appendLine(line.replace("%LOG_FILE%", invocationLog.normalize().absolutePath))
            }
        }

        destinationExecutableFile.setExecutable(true)

        return HelmExecutableParameter(destinationExecutableFile)
    }

    private fun getAbsoluteNormalizedPath(file: File): String {
        return file
            .absoluteFile
            .normalize()
            .path
            .replace('\\', '/')
    }

    class HelmExecutableParameter(
        path: File
    ) {
        val parameterValue = let {
            val normalizedFilePath = getAbsoluteNormalizedPath(path)

            "-Phelm.executable=${normalizedFilePath}"
        }
    }
}