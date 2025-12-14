package io.github.build.extensions.oss.gradle.plugins.helm.util

import io.kotest.matchers.equals.shouldBeEqual
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.util.concurrent.CompletableFuture

internal class FileLockTest {
    @TempDir
    private lateinit var tempFolder: File

    private lateinit var fileToLock: File

    @BeforeEach
    fun setup() {
        tempFolder.mkdirs()
        fileToLock = File(tempFolder, "text.txt")
        fileToLock.createNewFile()
    }

    @Test
    fun twoFunctionsArentAbleToUpdateFile() {
        runBlocking {
            val thread2IsReady = CompletableDeferred<Unit>()

            val taskIndices = 0..2

            val parallelTasks = taskIndices.map { taskIndex ->
                async {
                    // don't start until all tasks have been created
                    thread2IsReady.await()
                    withLockFile(fileToLock) {
                        taskIndex
                    }
                }
            }

            thread2IsReady.complete(Unit)

            val taskResults = parallelTasks.awaitAll().toSet()

            taskResults shouldBeEqual taskIndices.toSet()
        }
    }

    @Test
    fun multipleThreadsSynchronizedWhenUsingTheSameFile(){
        val taskIndices = 0..2
        val parallelTasks = taskIndices.map { taskIndex ->
            CompletableFuture.supplyAsync {
                withLockFile(fileToLock) {
                    Thread.sleep(100)
                    taskIndex
                }
            }
        }

        val taskResults = parallelTasks.map(CompletableFuture<Int>::join).toSet()

        taskResults shouldBeEqual taskIndices.toSet()
    }

    @Test
    fun twoThreadsSynchronizedWhenUsingTheSameFileThroughADifferentPath(){
        val parallelTasks = mutableListOf<CompletableFuture<Int>>(
            CompletableFuture.supplyAsync {
                withLockFile(File(tempFolder, "../" + tempFolder.name + "/text.txt")) {
                    Thread.sleep(1000)
                    1
                }
            },
            CompletableFuture.supplyAsync {
                withLockFile(fileToLock) {
                    Thread.sleep(1000)
                    2
                }
            }
        )

        val taskResults = parallelTasks.map(CompletableFuture<Int>::join).toSet()

        taskResults shouldBeEqual setOf(1,2)
    }

    @Test
    fun fileLockWorksForNewFile(){
        val file = File(tempFolder, "newLockFile.lock")
        file.delete()
        val parallelTask = CompletableFuture.supplyAsync {
                withLockFile(file) {
                    Thread.sleep(100)
                    1
                }
            }

        val taskResults = parallelTask.get()

        taskResults shouldBeEqual 1
    }
}