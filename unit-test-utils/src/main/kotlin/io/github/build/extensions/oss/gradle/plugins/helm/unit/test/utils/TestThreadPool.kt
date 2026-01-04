package io.github.build.extensions.oss.gradle.plugins.helm.unit.test.utils

import java.util.concurrent.Executors

/**
 * Shared test pool used in tests only
 */
object TestThreadPool {
    val threalPool = Executors.newFixedThreadPool(4)
}