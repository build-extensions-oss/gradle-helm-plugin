package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils

import java.io.File
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer

internal class DirectoryDispatcher(directory: File) : Dispatcher() {

    private val rootDirectory = directory.canonicalFile

    override fun dispatch(request: RecordedRequest): MockResponse {
        val relativePath = request.requestUrl?.encodedPath?.removePrefix("/")
            ?: return MockResponse().setResponseCode(400)
        val requestedFile = rootDirectory.resolve(relativePath).canonicalFile

        if (!requestedFile.toPath().startsWith(rootDirectory.toPath()) || !requestedFile.isFile) {
            return MockResponse().setResponseCode(404)
        }

        return MockResponse()
            .setResponseCode(200)
            .setBody(Buffer().write(requestedFile.readBytes()))
    }
}
