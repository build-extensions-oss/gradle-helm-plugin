package io.github.build.extensions.oss.gradle.plugins.helm.util

import io.kotest.matchers.shouldBe
import java.net.URI
import java.util.stream.Stream
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OciRegistryTest {

    @ParameterizedTest
    @MethodSource("createRepositoryInputs")
    fun `should recognize OCI registries`(case: RepositoryTestCase) {
        URI(case.url).isOciRegistry shouldBe case.expectedOci
    }

    @ParameterizedTest
    @MethodSource("createHostInputs")
    fun `should extract the registry host for helm registry login`(case: HostTestCase) {
        URI(case.url).ociRegistryHost shouldBe case.expectedHost
    }

    @Suppress("UnusedPrivateMember")
    private fun createRepositoryInputs(): Stream<Arguments> {
        return listOf(
            RepositoryTestCase("oci://registry.example.com/helm-local", true),
            RepositoryTestCase("OCI://registry.example.com", true),
            RepositoryTestCase("https://charts.example.com/", false),
            RepositoryTestCase("http://localhost:8080/charts", false),
            RepositoryTestCase("file:///tmp/charts", false)
        ).map { Arguments.arguments(it) }
            .stream()
    }

    @Suppress("UnusedPrivateMember")
    private fun createHostInputs(): Stream<Arguments> {
        return listOf(
            HostTestCase("oci://registry.example.com", "registry.example.com"),
            HostTestCase("oci://other.registry.com/helm-local/nested", "other.registry.com"),
            HostTestCase("oci://registry.example.com:5000/helm-local", "registry.example.com:5000"),
            HostTestCase("oci://user@registry.example.com/helm-local", "registry.example.com")
        ).map { Arguments.arguments(it) }
            .stream()
    }

    data class RepositoryTestCase(val url: String, val expectedOci: Boolean)

    data class HostTestCase(val url: String, val expectedHost: String)
}
