package io.github.build.extensions.oss.gradle.plugins.helm.command.internal

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RegistryLoginTest {

    @ParameterizedTest
    @MethodSource("createInputs")
    fun `should match chart dependency repositories pulled from the registry`(case: TestCase) {
        RegistryLogin(case.host).servesRepository(case.repository) shouldBe case.expectedMatch
    }

    @Test
    fun `should never print the password`() {
        val login = RegistryLogin("registry.example.com", username = "ci-user", password = "s3cr3t")

        login.toString() shouldNotContain "s3cr3t"
        login.toString() shouldContain "ci-user"
        login.toString() shouldContain "registry.example.com"
    }

    @Suppress("UnusedPrivateMember")
    private fun createInputs(): Stream<Arguments> {
        return listOf(
            TestCase("registry.example.com", "oci://registry.example.com/helm-local", true),
            TestCase("registry.example.com", "oci://REGISTRY.example.com/helm-local", true),
            TestCase("registry.example.com:5000", "oci://registry.example.com:5000/helm-local", true),
            // a different port is a different registry
            TestCase("registry.example.com", "oci://registry.example.com:5000/helm-local", false),
            TestCase("registry.example.com", "oci://other.example.com/helm-local", false),
            // classic repositories never need a registry login
            TestCase("registry.example.com", "https://registry.example.com/charts", false),
            // repository aliases and local charts
            TestCase("registry.example.com", "@artifactory", false),
            TestCase("registry.example.com", "file://../common", false),
            TestCase("registry.example.com", "not a uri", false)
        ).map { Arguments.arguments(it) }
            .stream()
    }

    data class TestCase(val host: String, val repository: String, val expectedMatch: Boolean)
}
