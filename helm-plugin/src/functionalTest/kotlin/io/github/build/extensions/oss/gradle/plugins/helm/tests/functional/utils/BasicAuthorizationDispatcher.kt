package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils

import okhttp3.Credentials
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * OkHttp authorization dispatcher to block unauthorized connectivity from Helm (and to test we can find that out).
 */
internal class BasicAuthorizationDispatcher(
    private val delegate: Dispatcher,
) : Dispatcher() {

    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.headers["Authorization"] != Credentials.basic(REPOSITORY_USERNAME, REPOSITORY_PASSWORD)) {
            return MockResponse()
                .setResponseCode(401)
                .setHeader("WWW-Authenticate", "Basic realm=\"helm-repository\"")
        }

        return delegate.dispatch(request)
    }

    companion object {
        const val REPOSITORY_USERNAME = "repository-user"
        const val REPOSITORY_PASSWORD = "repository-password"
    }
}
