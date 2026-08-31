package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.AuthorizationConfigurationWay
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.BasicAuthorizationDispatcher
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.DirectoryDispatcher
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.RepositoryAuthorization
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.RepositoryCertificates
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.RepositoryTestParameters
import io.kotest.matchers.file.exist
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import java.io.File
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The goal of this test is to check if we can render a chart which has dependencies
 */
internal class HelmRenderWithDependenciesTest {

    // the folder to the helm chart which we are going to render
    private val sourceDirectory = File("./src/functionalTest/resources/test/render-with-repository")

    @TempDir
    private lateinit var testProjectDir: File

    private lateinit var helmRegistryDirectory: File

    // the okhttp server will play a role of helm registry
    private lateinit var server: MockWebServer

    private lateinit var repositoryCertificates: RepositoryCertificates.Configuration

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)

        helmRegistryDirectory = testProjectDir.resolve("helm-registry")
        check(helmRegistryDirectory.mkdir()) {
            "Unable to create Helm registry directory at $helmRegistryDirectory"
        }
    }

    @AfterEach
    fun stopServer() {
        if (::server.isInitialized) {
            server.close()
        }
    }

    @ParameterizedTest
    @MethodSource("io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.RepositoryTestParameters#getAll")
    fun helmRenderShouldResolveChartFromRepository(parameters: RepositoryTestParameters) {
        // given
        // step 1 - prepare a dependent chart. See test HelmDependentChartQuickRenderTest which does a smoke check of this chart
        packageDependentChart(parameters.gradleRunnerParameters)
        // step 2 - copy the final tgz file and the helm index into a separate folder.
        copyRepositoryIndex()
        // step 3 - start http server, which would map the local holder to http. So, basically we don't keep a logic in this server
        startHelmRegistry(parameters.authorization)

        // and now we are ready to proceed with the real chart
        val gradleRunner = GradleRunnerProvider.createRunner(
            parameters = parameters.gradleRunnerParameters,
            projectDir = testProjectDir.resolve("chart-with-dependencies"),
            arguments = buildList {
                add("helmRender")
                add("-PhelmRepositoryUrl=${server.url("/")}")
                add("-PhelmRepositoryConfigurationWay=${parameters.authorizationConfigurationWay}")
                addAll(repositoryAuthorizationArguments(parameters))
                add("--stacktrace")
            },
        )

        // when
        val result = gradleRunner.build()

        // then
        result.output shouldContain "BUILD SUCCESSFUL"
        result.output shouldContain "Task :helmAddTestRepository"
        result.output shouldContain "Task :helmUpdateMainChartDependencies"
        result.output shouldContain "Task :helmRenderMainChartDefaultRendering"

        val renderedChart = testProjectDir.resolve(
            "chart-with-dependencies/build/helm/render/main/default/chart-with-dependencies",
        )
        // validate that the final chart had been rendered
        val renderedServiceAccount = renderedChart.resolve("templates/service-account.yaml")
        renderedServiceAccount should exist()
        renderedServiceAccount.readText() shouldContain "kind: ServiceAccount"

        // validate that the dependent job had been rendered as well (proves that we consumed the dependent chart)
        val renderedDependentJob = renderedChart.resolve("charts/dependent-chart/templates/job.yaml")
        renderedDependentJob should exist()
        renderedDependentJob.readText() shouldContain "kind: Job"
    }

    private fun packageDependentChart(parameters: DefaultGradleRunnerParameters) {
        val result = GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir.resolve("dependent-chart"),
            arguments = listOf("copyDependentChartToHelmRegistry", "--stacktrace"),
        ).build()

        result.output shouldContain "BUILD SUCCESSFUL"
        helmRegistryDirectory.resolve("dependent-chart-1.2.3.tgz") should exist()
    }

    private fun copyRepositoryIndex() {
        testProjectDir.resolve("repository-index.yaml")
            .copyTo(helmRegistryDirectory.resolve("index.yaml"))
    }

    private fun startHelmRegistry(authorization: RepositoryAuthorization) {
        val directoryDispatcher = DirectoryDispatcher(helmRegistryDirectory)
        server = MockWebServer().apply {
            dispatcher = when (authorization) {
                RepositoryAuthorization.UserPassword -> BasicAuthorizationDispatcher(directoryDispatcher)
                RepositoryAuthorization.Anonymous,
                RepositoryAuthorization.Certificate -> directoryDispatcher
            }

            if (authorization == RepositoryAuthorization.Certificate) {
                repositoryCertificates = RepositoryCertificates.create(testProjectDir)
                useHttps(repositoryCertificates.serverSocketFactory, false)
                requireClientAuth()
            }

            start()
        }
    }

    private fun repositoryAuthorizationArguments(parameters: RepositoryTestParameters): List<String> = buildList {
        // we are going to check multiple authorization options and multiple configuration ones.
        // e.g. the goal is to check all combinations.
        // Check https://build-extensions-oss.github.io/gradle-helm-plugin/#_configuring_repository_credentials
        // and https://build-extensions-oss.github.io/gradle-helm-plugin/#_repositories
        when (parameters.authorizationConfigurationWay) {
            AuthorizationConfigurationWay.GradleProperties -> {
                val prefix = "helm.repositories.$REPOSITORY_NAME"
                add("-P$prefix.url=${server.url("/")}")

                when (parameters.authorization) {
                    RepositoryAuthorization.Anonymous -> Unit
                    RepositoryAuthorization.UserPassword -> {
                        add(
                            "-P$prefix.credentials.username=" +
                                BasicAuthorizationDispatcher.REPOSITORY_USERNAME
                        )
                        add(
                            "-P$prefix.credentials.password=" +
                                BasicAuthorizationDispatcher.REPOSITORY_PASSWORD
                        )
                    }
                    RepositoryAuthorization.Certificate -> {
                        add("-P$prefix.credentials.certificateFile=${repositoryCertificates.clientCertificateFile}")
                        add("-P$prefix.credentials.keyFile=${repositoryCertificates.clientKeyFile}")
                    }
                }
            }
            AuthorizationConfigurationWay.GradleCode -> {
                add("-PhelmRepositoryAuthorization=${parameters.authorization}")

                when (parameters.authorization) {
                    RepositoryAuthorization.Anonymous -> Unit
                    RepositoryAuthorization.UserPassword -> Unit
                    RepositoryAuthorization.Certificate -> {
                        add("-PhelmRepositoryCertificateFile=${repositoryCertificates.clientCertificateFile}")
                        add("-PhelmRepositoryKeyFile=${repositoryCertificates.clientKeyFile}")
                    }
                }
            }
        }

        if (parameters.authorization == RepositoryAuthorization.Certificate) {
            add("-PhelmRepositoryCaFile=${repositoryCertificates.caCertificateFile}")
        }
    }

    private companion object {
        const val REPOSITORY_NAME = "test"
    }
}
