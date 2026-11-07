package io.github.build.extensions.oss.gradle.plugins.helm.command.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.Test
import java.time.Duration


internal class HelmCommandRetryTest {

    @Test
    fun returnsTheResultOfTheFirstSuccessfulAttempt() {

        var attempts = 0

        val result = HelmCommandRetry.execute(attempts = 3, delay = Duration.ZERO) {
            attempts++
            "done"
        }

        result shouldBeEqual "done"
        attempts shouldBeEqual 1
    }


    @Test
    fun retriesUntilAnAttemptSucceeds() {

        var attempts = 0

        val result = HelmCommandRetry.execute(attempts = 3, delay = Duration.ZERO) {
            attempts++
            if (attempts < 3) throw IllegalStateException("transient failure") else "done"
        }

        result shouldBeEqual "done"
        attempts shouldBeEqual 3
    }


    @Test
    fun reportsEachFailedAttempt() {

        val reported = mutableListOf<Int>()
        var attempts = 0

        HelmCommandRetry.execute(
            attempts = 3,
            delay = Duration.ZERO,
            onRetry = { attempt, _ -> reported.add(attempt) }
        ) {
            attempts++
            if (attempts < 3) throw IllegalStateException("transient failure") else "done"
        }

        reported shouldBeEqual listOf(1, 2)
    }


    @Test
    fun reportsTheLastFailureOnceTheAttemptsAreExhausted() {

        var attempts = 0

        val failure = shouldThrow<IllegalStateException> {
            HelmCommandRetry.execute(attempts = 3, delay = Duration.ZERO) {
                attempts++
                throw IllegalStateException("attempt $attempts")
            }
        }

        failure.message.orEmpty() shouldBeEqual "attempt 3"
        attempts shouldBeEqual 3
    }


    @Test
    fun rejectsAnAttemptCountBelowOne() {

        shouldThrow<IllegalArgumentException> {
            HelmCommandRetry.execute(attempts = 0, delay = Duration.ZERO) { "done" }
        }
    }


    @Test
    fun rejectsANegativeRetryDelay() {

        shouldThrow<IllegalArgumentException> {
            HelmCommandRetry.execute(attempts = 3, delay = Duration.ofSeconds(-1)) { "done" }
        }
    }
}
