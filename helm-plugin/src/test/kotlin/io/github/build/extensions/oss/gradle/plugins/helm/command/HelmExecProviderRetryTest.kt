package io.github.build.extensions.oss.gradle.plugins.helm.command

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import java.io.File
import java.lang.reflect.Proxy
import java.time.Duration
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.JavaExecSpec
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


internal class HelmExecProviderRetryTest {

    @TempDir
    private lateinit var stdoutCaptureDir: File


    @Test
    fun retriesAFailingCommandUntilItSucceeds() {

        val execOperations = FailingExecOperations(failures = 2)

        execOperations.execHelmWithRetry(attempts = 3)

        execOperations.invocations shouldBeEqual 3
    }


    @Test
    fun reportsTheFailureOnceTheAttemptsAreExhausted() {

        val execOperations = FailingExecOperations(failures = Int.MAX_VALUE)

        shouldThrow<GradleException> {
            execOperations.execHelmWithRetry(attempts = 3)
        }

        execOperations.invocations shouldBeEqual 3
    }


    @Test
    fun runsTheCommandOnceWhenItSucceeds() {

        val execOperations = FailingExecOperations(failures = 0)

        execOperations.execHelmWithRetry(attempts = 3)

        execOperations.invocations shouldBeEqual 1
    }


    private fun FailingExecOperations.execHelmWithRetry(attempts: Int) {
        val support = HelmExecProviderSupport(
            this, stdoutCaptureDir, null, object : HelmOptions {}, emptyList()
        )

        support.execHelmWithRetry("repo", "update", attempts, Duration.ZERO, null)
    }


    /**
     * A Helm execution that fails the first [failures] invocations, as a transient network failure does.
     *
     * The path under test is the synchronous one, which Gradle selects when it runs without a daemon: it throws from
     * the invocation itself rather than from a worker's [org.gradle.workers.WorkerExecutor.await].
     */
    private class FailingExecOperations(private val failures: Int) : ExecOperations {

        var invocations: Int = 0
            private set


        override fun exec(action: Action<in ExecSpec>): ExecResult {
            invocations++
            action.execute(EmptyExecSpec.newInstance(mutableListOf()))
            if (invocations <= failures) {
                throw GradleException("simulated failure $invocations")
            }
            return EmptyExecResult
        }


        override fun javaexec(action: Action<in JavaExecSpec>): ExecResult =
            throw UnsupportedOperationException("test does not run a Java executable")
    }


    /**
     * An [ExecSpec] that only collects the command line, which is all the retry path builds before it invokes Helm.
     *
     * The interface's setters are fluent, so a dynamic proxy returns itself for them rather than a hand-written
     * implementation of every method.
     */
    private object EmptyExecSpec {

        fun newInstance(args: MutableList<String>): ExecSpec {
            val spec = Proxy.newProxyInstance(
                ExecSpec::class.java.classLoader,
                arrayOf(ExecSpec::class.java)
            ) { proxy, method, methodArgs ->
                when (method.name) {
                    "args" -> {
                        methodArgs?.firstOrNull()?.let { argument ->
                            if (argument is Iterable<*>) {
                                args.addAll(argument.filterIsInstance<String>())
                            } else {
                                args.add(argument.toString())
                            }
                        }
                        proxy
                    }
                    "getArgs" -> args
                    "getCommandLine" -> (listOf("helm") + args).toMutableList()
                    "getEnvironment" -> mutableMapOf<String, Any>()
                    "getWorkingDir" -> File(".")
                    "getExecutable" -> "helm"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === methodArgs?.firstOrNull()
                    "toString" -> "ExecSpec"
                    else -> if (method.returnType == Void.TYPE) null else proxy
                }
            }

            return spec as ExecSpec
        }
    }


    private object EmptyExecResult : ExecResult {

        override fun getExitValue(): Int = 0
        override fun assertNormalExitValue(): ExecResult = this
        override fun rethrowFailure(): ExecResult = this
    }
}
