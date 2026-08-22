package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class RepositoryPropertiesTest {
    private val sourceDirectory = File("./src/functionalTest/resources/test/repository-properties")

    @TempDir
    private lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource(
        "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
            "DefaultGradleRunnerParameters#getDefaultParameterSet"
    )
    fun shouldConfigureRepositoriesFromPrefixedProjectProperties(parameters: DefaultGradleRunnerParameters) {
        val result = GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf(
                "help",
                "--stacktrace",
                "--warning-mode=all",
                "-Phelm.repositories.passwordRepo.url=https://password.example.com/charts",
                "-Phelm.repositories.passwordRepo.credentials.username=test-user",
                "-Phelm.repositories.passwordRepo.credentials.password=test-password",
                "-Phelm.repositories.certificateRepo.url=https://certificate.example.com/charts",
                "-Phelm.repositories.certificateRepo.credentials.certificateFile=client.pem",
                "-Phelm.repositories.certificateRepo.credentials.keyFile=client-key.pem"
            )
        ).build()

        result.output shouldContain "Repository project properties verified"
        result.output shouldNotContain "Project.getProperties"
    }
}
