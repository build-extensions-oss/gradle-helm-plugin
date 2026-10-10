package io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec

import java.net.HttpURLConnection
import java.net.URI
import kotlin.system.exitProcess


/**
 * Stand-in for the real Helm client executable.
 *
 * This runs in a separate JVM, started by the small wrapper script that
 * [ExecMockServer.MockRegistration.writeLauncherScript] puts in the project directory. It forwards its own
 * arguments and environment to the [execMockServer], and echoes the response body on stdout — which is what
 * the exec mock hands back to the test as the "output" of the fake Helm client.
 *
 * The wrapper passes the mock id, the server port and the path of the wrapper itself ahead of the arguments
 * that Helm was actually invoked with.
 *
 * This class deliberately uses nothing but the JDK and the Kotlin standard library. The wrapper script puts
 * only those two entries on the classpath, which keeps the generated command line short enough to stay well
 * inside the Windows command-line length limit.
 */
object ExecMockLauncher {

    private const val FIXED_ARG_COUNT = 3
    private const val HTTP_BAD_REQUEST = 400


    @JvmStatic
    fun main(args: Array<String>) {

        require(args.size >= FIXED_ARG_COUNT) {
            "Expected at least $FIXED_ARG_COUNT arguments (mockId, port, executable) but got ${args.toList()}"
        }

        val mockId = args[0]
        val port = args[1].toInt()
        val executable = args[2]
        val helmArgs = args.drop(FIXED_ARG_COUNT)

        val statusCode = post(port, payload(mockId, executable, helmArgs))

        // Mirror `curl -f`: a failed request must make the fake executable fail, so that tests which
        // expect a non-zero exit code from the Helm client still get one.
        exitProcess(if (statusCode >= HTTP_BAD_REQUEST) 1 else 0)
    }


    private fun payload(mockId: String, executable: String, helmArgs: List<String>): String =
        buildString {
            append('{')
            append(jsonString("executable")).append(':').append(jsonString(executable)).append(',')
            append(jsonString("mockId")).append(':').append(jsonString(mockId)).append(',')
            append(jsonString("args")).append(':')
            helmArgs.joinTo(this, separator = ",", prefix = "[", postfix = "]") { jsonString(it) }
            append(',')
            append(jsonString("env")).append(':')
            System.getenv().entries.joinTo(this, separator = ",", prefix = "{", postfix = "}") { (name, value) ->
                "${jsonString(name)}:${jsonString(value)}"
            }
            append(',')
            // Gradle closes the standard input of a process unless a task writes to it, so this never blocks
            append(jsonString("stdin")).append(':').append(jsonString(System.`in`.readBytes().decodeToString()))
            append('}')
        }


    /**
     * Posts the payload to the mock server and prints the response body on stdout.
     *
     * @return the HTTP status code
     */
    private fun post(port: Int, payload: String): Int {

        val connection = (URI("http://localhost:$port").toURL().openConnection() as HttpURLConnection)
            .apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            }

        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val statusCode = connection.responseCode
            val stream = if (statusCode >= HTTP_BAD_REQUEST) connection.errorStream else connection.inputStream
            stream?.use { print(it.reader(Charsets.UTF_8).readText()) }
            System.out.flush()

            return statusCode

        } finally {
            connection.disconnect()
        }
    }


    /**
     * Renders a string as a JSON string literal.
     *
     * Doing this here rather than in the wrapper script is the whole point of the launcher: quoting this
     * correctly in shell or batch is painful, and gets it wrong for values that contain backslashes (every
     * `XDG_*` path on Windows), `=` signs, or newlines.
     */
    private fun jsonString(value: String): String =
        buildString(value.length + 2) {
            append('"')
            for (char in value) {
                when {
                    char == '"' -> append("\\\"")
                    char == '\\' -> append("\\\\")
                    char == '\n' -> append("\\n")
                    char == '\r' -> append("\\r")
                    char == '\t' -> append("\\t")
                    char < ' ' -> append("\\u").append(char.code.toString(16).padStart(4, '0'))
                    else -> append(char)
                }
            }
            append('"')
        }
}
