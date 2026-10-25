package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * One helm call, as written by the recording executable (`helm-record.sh` / `helm-record.bat`).
 *
 * @property arguments the command line arguments, starting with the command
 * @property stdin the first line of the standard input the call received; empty if there was none
 */
internal class HelmInvocation(val arguments: List<String>, val stdin: String) {

    /**
     * The command and subcommand, e.g. `registry login`.
     */
    val command: String
        get() = arguments.take(2).joinToString(" ")

    infix fun shouldHaveOption(option: Pair<String, String>) {
        val (name, value) = option
        arguments shouldContain name
        arguments.getOrNull(arguments.indexOf(name) + 1) shouldBe value
    }

    companion object {

        /**
         * Reads the log: one line per helm call - the arguments, then `STDIN=<first line>`, then
         * `KUBECONFIG=<value>`.
         */
        fun readAll(invocationLog: File): List<HelmInvocation> =
            if (!invocationLog.exists()) {
                emptyList()
            } else {
                invocationLog.readLines()
                    .filter { it.isNotBlank() }
                    .map { line ->
                        val beforeKubeConfig = line.substringBeforeLast(" KUBECONFIG=")
                        HelmInvocation(
                            arguments = beforeKubeConfig.substringBeforeLast(" STDIN=").trim().split(Regex("\\s+")),
                            stdin = beforeKubeConfig.substringAfterLast(" STDIN=").trim()
                        )
                    }
            }
    }
}
