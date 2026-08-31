package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters.Companion.allWithoutHelmVersion
import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

/**
 * The authorization which must be configured and used for the particular repository.
 * @see io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmAddRepository::RepositoryConfig
 */
enum class RepositoryAuthorization {
    Anonymous,
    UserPassword,
    Certificate
}

enum class AuthorizationConfigurationWay {
    // an authorization is supplied via Gralde properites. See `helm.repository.<name>.credentials.certificateFile` from the documentation
    GradleProperties,
    // an authorization is supplied via Gradle configuration. See `credentials(CertificateCredentials)` from the documentation.
    GradleCode
}

/**
 * Test parameters - aggregate how to run Gradle and how to protect the repository
 */
data class RepositoryTestParameters(
    val gradleRunnerParameters: DefaultGradleRunnerParameters,
    val authorizationConfigurationWay: AuthorizationConfigurationWay,
    val authorization: RepositoryAuthorization
) {
    companion object {
        private val all = DefaultGradleRunnerParameters.all.flatMap { gradleRunnerParameters ->
            AuthorizationConfigurationWay.entries.flatMap { authorizationConfigurationWay ->
                RepositoryAuthorization.entries.map {
                    it to RepositoryTestParameters(
                        gradleRunnerParameters = gradleRunnerParameters,
                        authorization = it,
                        authorizationConfigurationWay = authorizationConfigurationWay
                    )
                }
            }
        }

        /**
         * Return all parameters to test
         */
        @JvmStatic
        fun getAll(): Stream<Arguments> {
            return all.map { Arguments.of(it) }
                .stream()
        }
    }
}