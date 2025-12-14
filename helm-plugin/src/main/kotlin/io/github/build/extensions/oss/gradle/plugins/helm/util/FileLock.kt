package io.github.build.extensions.oss.gradle.plugins.helm.util

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


private val locksByLockFile = ConcurrentHashMap<Path, ReentrantLock>()


internal fun <T> withLockFile(file: File, block: () -> T): T {
    if (!file.exists()) {
        file.parentFile.mkdirs()
        file.createNewFile()
    }
    // File locks won't work within the same JVM, we have to synchronize access to the lockfile
    // with a ReentrantLock
    return locksByLockFile.computeIfAbsent(file.toPath().toRealPath()) { ReentrantLock() }
        .withLock {
            file.parentFile.mkdirs()
            RandomAccessFile(file, "rw").channel.use { channel ->
                var res :T
                while(true){
                    try {
                        val fileLock = channel.tryLock()
                        if (fileLock != null) {
                            res =  fileLock.use { block() }
                            break
                        }
                    } catch (ex: OverlappingFileLockException){
                        Thread.sleep(100)
                    }
                }
                res
            }
        }
}
