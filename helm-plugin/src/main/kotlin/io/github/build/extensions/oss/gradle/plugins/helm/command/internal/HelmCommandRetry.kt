package io.github.build.extensions.oss.gradle.plugins.helm.command.internal

import java.time.Duration


/**
 * Re-runs a Helm CLI invocation that can fail transiently, a bounded number of times.
 *
 * <p>The logic holds no Gradle state, so it is testable without invoking the Helm executable.
 */
internal object HelmCommandRetry {

    /**
     * The attempts a fetch task makes when the build configures none.
     *
     * One attempt keeps the historical behaviour: a task retries only when a build raises it, which is what postpones
     * the failure by another attempt's timeout.
     */
    const val DEFAULT_ATTEMPTS = 1


    /**
     * The pause between attempts, long enough for a transient network failure to clear.
     */
    val DEFAULT_DELAY: Duration = Duration.ofSeconds(10)


    /**
     * Returns [action]'s result, re-running it when it throws until [attempts] attempts have been made, pausing
     * [delay] between them and reporting each transient failure through [onRetry]. The last attempt's failure
     * propagates, so a persistent failure is still reported.
     *
     * @param attempts the number of attempts, at least one
     * @param delay the pause between attempts; [Duration.ZERO] retries without pausing
     * @param onRetry called after a failed attempt, with the attempt's number and its failure
     * @param action the invocation to run
     */
    fun <T> execute(
        attempts: Int,
        delay: Duration,
        onRetry: (attempt: Int, failure: Exception) -> Unit = { _, _ -> },
        action: () -> T,
    ): T {
        require(attempts >= 1) { "A Helm command needs at least one attempt, but $attempts were configured." }
        require(!delay.isNegative) { "A retry delay cannot be negative, but $delay was configured." }

        repeat(attempts - 1) { index ->
            try {
                return action()
            } catch (e: Exception) {
                onRetry(index + 1, e)
                if (!delay.isZero) {
                    Thread.sleep(delay.toMillis())
                }
            }
        }

        return action()
    }
}
