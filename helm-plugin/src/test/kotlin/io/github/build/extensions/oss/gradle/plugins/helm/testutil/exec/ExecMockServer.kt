package io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread


interface ExecMockServer {

    fun registerMock(callback: Callback): MockRegistration


    interface MockRegistration {

        /**
         * Writes an executable stand-in for the Helm client at the given location.
         *
         * The file actually written may not be [location] itself — on Windows it needs a `.bat` extension,
         * because that is the only kind of script `CreateProcess` knows how to start.
         *
         * @param location the desired location of the fake executable
         * @return the file that should be used as the `executable`
         */
        fun writeLauncherScript(location: File): File

        fun unregister()
    }


    interface Callback {

        fun invocation(invocation: Invocation, stdoutWriter: PrintWriter)
    }
}


private val isWindows: Boolean =
    System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)


private val javaExecutable: String =
    File(File(System.getProperty("java.home"), "bin"), if (isWindows) "java.exe" else "java").absolutePath


/**
 * Keep JVM startup as cheap as possible: the fake executable is started once per mocked Helm invocation,
 * and it only ever makes a single HTTP call.
 */
private const val LAUNCHER_JVM_OPTIONS = "-XX:TieredStopAtLevel=1 -XX:+UseSerialGC"


/**
 * The classpath for [ExecMockLauncher] — its own output directory plus the Kotlin standard library, and
 * nothing else. Keeping this to two entries is what allows the wrapper to be a single short command line.
 */
private val launcherClasspath: String =
    listOf(ExecMockLauncher::class.java, Unit::class.java)
        .mapNotNull { it.protectionDomain?.codeSource?.location }
        .map { url -> runCatching { File(url.toURI()) }.getOrElse { File(url.path) }.absolutePath }
        .distinct()
        .also { check(it.isNotEmpty()) { "Unable to determine the classpath for ${ExecMockLauncher::class.java}" } }
        .joinToString(File.pathSeparator)


private class DefaultExecMockServer : ExecMockServer, AutoCloseable {

    private inner class MockRegistrationImpl(
        val id: String
    ) : ExecMockServer.MockRegistration {

        override fun writeLauncherScript(location: File): File {

            val scriptFile = if (isWindows) File(location.parentFile, location.name + ".bat") else location

            scriptFile.parentFile.mkdirs()
            scriptFile.writeText(if (isWindows) windowsScript() else posixScript())
            scriptFile.setExecutable(true)

            return scriptFile
        }


        /**
         * `%~f0` is the full path of the batch file itself, and `%*` forwards the arguments verbatim.
         *
         * The latter only holds because the test JVM sets `jdk.lang.Process.allowAmbiguousCommands=false`
         * (see the `test` task in _build.gradle.kts_). Java has to run a `.bat` through `cmd.exe`, and in
         * cmd's default mode the shell would eat `^` and split the command on `&` before the batch file
         * ever sees them.
         */
        private fun windowsScript(): String =
            "@echo off\r\n" +
                    "\"$javaExecutable\" $LAUNCHER_JVM_OPTIONS -cp \"$launcherClasspath\" " +
                    "${ExecMockLauncher::class.java.name} $id $portNumber \"%~f0\" %*\r\n"


        private fun posixScript(): String =
            "#!/bin/sh\n" +
                    "exec \"$javaExecutable\" $LAUNCHER_JVM_OPTIONS -cp \"$launcherClasspath\" " +
                    "${ExecMockLauncher::class.java.name} $id $portNumber " +
                    "\"${'$'}0\" \"${'$'}@\"\n"


        override fun unregister() {
            registrations.remove(id)
        }
    }


    private val registrations: MutableMap<String, ExecMockServer.Callback> = ConcurrentHashMap()


    private class MockDispatcher(
        private val registrations: Map<String, ExecMockServer.Callback>
    ) : Dispatcher() {

        override fun dispatch(request: RecordedRequest): MockResponse {
            val response = MockResponse()
            try {
                val input = request.body.readString(Charsets.UTF_8)
                val body = JSONObject(input)

                val mockId = body.getString("mockId")
                val callback = registrations.getValue(mockId)

                val invocation = DefaultInvocation(
                    executable = body.getString("executable"),
                    args = body.getJSONArray("args").toList().map { it.toString() },
                    environment = body.getJSONObject("env").toMap().mapValues { (_, v) -> v.toString() },
                    stdin = body.optString("stdin", "")
                )

                val stdoutWriter = StringWriter()
                PrintWriter(stdoutWriter).use { stdoutPrintWriter ->
                    callback.invocation(invocation, stdoutPrintWriter)
                }

                with(response) {
                    setResponseCode(200)
                    setHeader("Content-Type", "text/plain")
                    setBody(stdoutWriter.toString())
                }
            } catch (e: Exception) {
                response.setResponseCode(500)
                response.setBody(e.toString())
            }
            return response
        }
    }


    private val server = MockWebServer().apply {
        dispatcher = MockDispatcher(registrations)
        start()
        println("Exec mock server started on port $port")
    }


    private val portNumber = server.port


    override fun registerMock(callback: ExecMockServer.Callback): ExecMockServer.MockRegistration {
        val id = UUID.randomUUID().toString()
        registrations[id] = callback
        return MockRegistrationImpl(id)
    }


    override fun close() {
        server.close()
    }
}


val execMockServer: ExecMockServer by lazy {

    DefaultExecMockServer().also { server ->
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                server.close()
            }
        )
    }
}
